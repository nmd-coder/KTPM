package com.example.demo.service;

import com.example.demo.dto.ConversationDTO;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.HandoffQueue;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.ConversationStatus;
import com.example.demo.entity.enums.HandoffReason;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.HandoffQueueRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing conversation handoff from AI to staff
 * Handles queue management and staff assignment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HandoffService {

    private final HandoffQueueRepository handoffQueueRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final StaffPresenceService staffPresenceService;
    private final WebSocketService webSocketService;

    /**
     * Add conversation to handoff queue
     */
    @Transactional
    public HandoffQueue addToQueue(Long conversationId, HandoffReason reason, 
                                   String customerMessage, String aiContext, Integer priority) {
        log.info("Adding conversation {} to handoff queue - Reason: {}", conversationId, reason);
        
        // Check if already in queue
        Optional<HandoffQueue> existingHandoff = handoffQueueRepository.findByConversationId(conversationId);
        if (existingHandoff.isPresent()) {
            HandoffQueue handoff = existingHandoff.get();
            log.info("Conversation {} already in handoff queue, updating and re-notifying staff", conversationId);
            
            // Update with latest message and reason (e.g., new PII detected)
            handoff.setCustomerMessage(customerMessage);
            handoff.setAiContext(aiContext);
            handoff.setHandoffReason(reason);
            
            // Update priority if new reason is more urgent
            Integer newPriority = priority != null ? priority : determinePriority(reason);
            if (newPriority > handoff.getPriority()) {
                handoff.setPriority(newPriority);
                log.info("Increased priority to {} for conversation {}", newPriority, conversationId);
            }
            
            HandoffQueue updated = handoffQueueRepository.save(handoff);
            
            // Re-notify staff (important for new PII messages)
            notifyStaffAboutNewHandoff(updated);
            
            return updated;
        }

        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));

        // Create handoff queue entry
        HandoffQueue handoff = new HandoffQueue();
        handoff.setConversation(conversation);
        handoff.setHandoffReason(reason);
        handoff.setCustomerMessage(customerMessage);
        handoff.setAiContext(aiContext);
        handoff.setPriority(priority != null ? priority : determinePriority(reason));
        handoff.setEnqueuedAt(LocalDateTime.now());

        // Set tags based on reason
        handoff.setTags(generateTags(reason));

        HandoffQueue saved = handoffQueueRepository.save(handoff);
        
        log.info("Conversation {} added to handoff queue with priority {}", conversationId, saved.getPriority());
        
        // Notify staff about new conversation in queue
        notifyStaffAboutNewHandoff(saved);
        
        // Try auto-assign if enabled
        tryAutoAssign(saved.getId());
        
        return saved;
    }

    /**
     * Determine priority based on handoff reason
     */
    private Integer determinePriority(HandoffReason reason) {
        switch (reason) {
            case PAYMENT_ISSUE:
                return 8; // High priority
            case ORDER_INQUIRY:
                return 6; // Medium-high priority
            case PII_DETECTED:
                return 5; // Medium priority
            case LOW_CONFIDENCE:
            case COMPLEX_QUERY:
                return 3; // Normal priority
            case EXPLICIT_REQUEST:
                return 7; // High priority (customer asked)
            case AI_ERROR:
                return 4; // Medium priority
            default:
                return 5;
        }
    }

    /**
     * Generate tags based on handoff reason
     */
    private String[] generateTags(HandoffReason reason) {
        List<String> tags = new ArrayList<>();
        
        switch (reason) {
            case PAYMENT_ISSUE:
                tags.add("payment");
                tags.add("urgent");
                break;
            case ORDER_INQUIRY:
                tags.add("order");
                tags.add("support");
                break;
            case PII_DETECTED:
                tags.add("personal_info");
                break;
            case LOW_CONFIDENCE:
                tags.add("complex");
                break;
            case COMPLEX_QUERY:
                tags.add("complex");
                tags.add("advanced");
                break;
            case EXPLICIT_REQUEST:
                tags.add("customer_request");
                break;
            case AI_ERROR:
                tags.add("technical");
                break;
        }
        
        return tags.toArray(new String[0]);
    }

    /**
     * Assign conversation to staff
     */
    @Transactional
    public void assignToStaff(Long conversationId, Long staffId) {
        log.info("Assigning conversation {} to staff {}", conversationId, staffId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        // Update conversation
        conversation.setAssignedStaff(staff);
        conversation.setStatus(ConversationStatus.ASSIGNED);
        conversationRepository.save(conversation);
        
        // Update handoff queue
        Optional<HandoffQueue> handoffOpt = handoffQueueRepository.findByConversationId(conversationId);
        if (handoffOpt.isPresent()) {
            HandoffQueue handoff = handoffOpt.get();
            handoff.setAssignedToStaff(staff);
            handoff.setAssignedAt(LocalDateTime.now());
            handoffQueueRepository.save(handoff);
        }
        
        // Update staff presence
        staffPresenceService.incrementWorkload(staffId);
        
        log.info("Conversation {} assigned to staff {} successfully", conversationId, staffId);
        
        // Notify staff
        notifyStaffAboutAssignment(conversationId, staffId);
    }

    /**
     * Try to auto-assign conversation to available staff
     */
    @Transactional
    public boolean tryAutoAssign(Long handoffQueueId) {
        try {
            HandoffQueue handoff = handoffQueueRepository.findById(handoffQueueId)
                .orElse(null);
            
            if (handoff == null || handoff.getAssignedAt() != null) {
                return false;
            }

            // Get available staff
            List<User> availableStaff = staffPresenceService.getAvailableStaff();
            
            if (availableStaff.isEmpty()) {
                log.info("No available staff for auto-assignment");
                return false;
            }

            // Get staff with lowest workload
            User selectedStaff = availableStaff.get(0);
            
            log.info("Auto-assigning conversation {} to staff {}", 
                handoff.getConversation().getId(), selectedStaff.getId());
            
            assignToStaff(handoff.getConversation().getId(), selectedStaff.getId());
            
            return true;
            
        } catch (Exception e) {
            log.error("Error in auto-assignment", e);
            return false;
        }
    }

    /**
     * Get waiting queue (not yet assigned)
     */
    @Transactional(readOnly = true)
    public List<ConversationDTO> getWaitingQueue() {
        List<HandoffQueue> queue = handoffQueueRepository.findWaitingQueue();
        return queue.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get queue assigned to specific staff
     */
    @Transactional(readOnly = true)
    public List<ConversationDTO> getStaffQueue(Long staffId) {
        List<HandoffQueue> queue = handoffQueueRepository.findByAssignedToStaffId(staffId);
        return queue.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Resolve handoff (mark as completed)
     */
    @Transactional
    public void resolveHandoff(Long conversationId) {
        log.info("Resolving handoff for conversation {}", conversationId);
        
        Optional<HandoffQueue> handoffOpt = handoffQueueRepository.findByConversationId(conversationId);
        if (handoffOpt.isPresent()) {
            HandoffQueue handoff = handoffOpt.get();
            handoff.setResolvedAt(LocalDateTime.now());
            handoffQueueRepository.save(handoff);
            
            log.info("Handoff resolved for conversation {}", conversationId);
        }
    }

    /**
     * Get handoff statistics
     */
    @Transactional(readOnly = true)
    public HandoffStats getStats() {
        Long waiting = handoffQueueRepository.countWaiting();
        Long assigned = handoffQueueRepository.countAssigned();
        Double avgWaitTime = handoffQueueRepository.getAverageWaitTime();
        
        return new HandoffStats(waiting, assigned, avgWaitTime);
    }

    /**
     * Convert HandoffQueue to ConversationDTO
     */
    private ConversationDTO convertToDTO(HandoffQueue handoff) {
        Conversation conv = handoff.getConversation();
        ConversationDTO dto = new ConversationDTO();
        
        dto.setId(conv.getId());
        dto.setCustomerId(conv.getCustomer().getId());
        dto.setCustomerName(conv.getCustomer().getFullName());
        dto.setStatus(conv.getStatus());
        dto.setPriority(conv.getPriority());
        dto.setCreatedAt(conv.getCreatedAt());
        dto.setLastMessageAt(conv.getLastMessageAt());
        
        if (conv.getAssignedStaff() != null) {
            dto.setAssignedStaffId(conv.getAssignedStaff().getId());
            dto.setAssignedStaffName(conv.getAssignedStaff().getFullName());
        }
        
        // Add handoff-specific info
        dto.setHandoffReason(handoff.getHandoffReason() != null ? handoff.getHandoffReason().getDisplayName() : null);
        dto.setAiContext(handoff.getAiContext());
        dto.setEnqueuedAt(handoff.getEnqueuedAt());
        dto.setWaitTimeSeconds(handoff.getWaitTimeSeconds());
        
        return dto;
    }

    /**
     * Notify staff about new handoff
     */
    private void notifyStaffAboutNewHandoff(HandoffQueue handoff) {
        try {
            Conversation conversation = handoff.getConversation();
            Long conversationId = conversation.getId();
            String customerName = conversation.getCustomer().getFullName();
            HandoffReason reason = handoff.getHandoffReason();
            
            // 1. Send notification to handoff queue topic (for staff dashboard)
            webSocketService.notifyStaffNewHandoff(conversationId);
            
            // 2. Send notification to staff bell with appropriate message
            String notificationMessage;
            String notificationType;
            
            if (reason == HandoffReason.PII_DETECTED) {
                notificationMessage = String.format("🚨 %s cần hỗ trợ với thông tin cá nhân", customerName);
                notificationType = "pii_alert";
            } else if (reason == HandoffReason.PAYMENT_ISSUE) {
                notificationMessage = String.format("💳 %s cần hỗ trợ về thanh toán", customerName);
                notificationType = "new_conversation";
            } else if (reason == HandoffReason.ORDER_INQUIRY) {
                notificationMessage = String.format("📦 %s cần hỗ trợ về đơn hàng", customerName);
                notificationType = "new_conversation";
            } else {
                notificationMessage = String.format("🔔 %s cần hỗ trợ", customerName);
                notificationType = "new_conversation";
            }
            
            webSocketService.sendBroadcast(
                notificationMessage,
                notificationType,
                conversationId
            );
            
            log.info("Staff notifications sent for conversation {} (reason: {})", 
                conversationId, reason);
                
        } catch (Exception e) {
            log.error("Error notifying staff about new handoff", e);
        }
    }

    /**
     * Notify staff about assignment
     */
    private void notifyStaffAboutAssignment(Long conversationId, Long staffId) {
        try {
            webSocketService.notifyStaffAssignment(staffId, conversationId);
        } catch (Exception e) {
            log.error("Error notifying staff about assignment", e);
        }
    }

    /**
     * Inner class for handoff statistics
     */
    public static class HandoffStats {
        private final Long waitingCount;
        private final Long assignedCount;
        private final Double avgWaitTimeSeconds;

        public HandoffStats(Long waitingCount, Long assignedCount, Double avgWaitTimeSeconds) {
            this.waitingCount = waitingCount;
            this.assignedCount = assignedCount;
            this.avgWaitTimeSeconds = avgWaitTimeSeconds;
        }

        public Long getWaitingCount() {
            return waitingCount;
        }

        public Long getAssignedCount() {
            return assignedCount;
        }

        public Double getAvgWaitTimeSeconds() {
            return avgWaitTimeSeconds;
        }

        public String getAvgWaitTimeFormatted() {
            if (avgWaitTimeSeconds == null) {
                return "N/A";
            }
            long minutes = Math.round(avgWaitTimeSeconds / 60);
            return minutes + " phút";
        }
    }
}

