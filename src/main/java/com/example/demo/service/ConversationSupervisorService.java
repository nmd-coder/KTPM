package com.example.demo.service;

import com.example.demo.entity.Conversation;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.ConversationStatus;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service quản lý việc trao lại conversation cho AI sau khi staff bấm nút "AI"
 * Chờ 30 giây, nếu khách không nhắn tin thì trao lại cho AI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationSupervisorService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebSocketService webSocketService;
    
    // Map lưu conversation đang chờ trao lại cho AI: conversationId -> deadline
    private final ConcurrentMap<Long, LocalDateTime> aiResumePending = new ConcurrentHashMap<>();
    
    private static final int WAIT_SECONDS = 30;
    
    /**
     * Đưa conversation vào hàng đợi trao lại cho AI
     * Gửi thông báo cho khách và bắt đầu đếm 30 giây
     */
    @Transactional
    public void queueReturnToAi(Long conversationId) {
        try {
            Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
            
            if (conversation.getAssignedStaff() == null) {
                log.warn("Cannot queue return to AI: conversation {} has no assigned staff", conversationId);
                return;
            }
            
            // Đưa vào hàng đợi
            LocalDateTime deadline = LocalDateTime.now().plusSeconds(WAIT_SECONDS);
            aiResumePending.put(conversationId, deadline);
            
            log.info("🤖 Queued conversation {} for AI return (deadline: {})", conversationId, deadline);
            
            // Gửi system message thông báo cho khách
            User systemUser = getOrCreateSystemUser();
            Message systemMessage = new Message();
            systemMessage.setConversationId(conversationId);
            systemMessage.setSender(systemUser);
            systemMessage.setReceiver(conversation.getCustomer());
            systemMessage.setContent(
                "Nếu bạn không còn thắc mắc nào cần nhân viên hỗ trợ trực tiếp, " +
                "sau " + WAIT_SECONDS + " giây nữa Hoa AI sẽ tiếp tục tư vấn cho bạn. " +
                "Nếu trong " + WAIT_SECONDS + " giây này bạn nhắn tin, nhân viên sẽ tiếp tục hỗ trợ bạn nhé! 💬"
            );
            systemMessage.setIsAiGenerated(false);
            systemMessage.setSentAt(LocalDateTime.now());
            Message savedMessage = messageRepository.save(systemMessage);
            
            // Gửi message qua WebSocket cho customer
            com.example.demo.dto.ChatMessageDTO messageDTO = new com.example.demo.dto.ChatMessageDTO();
            messageDTO.setId(savedMessage.getId());
            messageDTO.setConversationId(conversationId);
            messageDTO.setSenderId(systemUser.getId());
            messageDTO.setSenderName("Hệ thống");
            messageDTO.setContent(savedMessage.getContent());
            messageDTO.setSentAt(savedMessage.getSentAt());
            messageDTO.setIsAiGenerated(false);
            
            webSocketService.sendChatMessage(messageDTO);
            
            // Gửi conversation update
            webSocketService.sendConversationUpdate(conversationId, "system_notice", null);
            
            log.info("✅ Sent AI return notice to customer in conversation {}", conversationId);
            
        } catch (Exception e) {
            log.error("Error queuing conversation for AI return: {}", e.getMessage(), e);
            aiResumePending.remove(conversationId);
        }
    }
    
    /**
     * Hủy việc trao lại cho AI nếu khách nhắn tin trong 30 giây
     * Được gọi từ ChatService khi nhận tin nhắn từ CUSTOMER
     * 
     * @return true nếu đã hủy thành công (conversation đang trong queue)
     */
    @Transactional
    public boolean cancelIfPendingByCustomerMessage(Long conversationId) {
        LocalDateTime deadline = aiResumePending.get(conversationId);
        
        if (deadline == null) {
            // Không có trong queue
            return false;
        }
        
        // Kiểm tra nếu chưa hết hạn
        if (LocalDateTime.now().isBefore(deadline)) {
            // Hủy việc trao lại cho AI
            aiResumePending.remove(conversationId);
            
            log.info("🚫 Cancelled AI return for conversation {} (customer sent message)", conversationId);
            
            try {
                // Gửi system message thông báo
                Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
                if (conversation != null) {
                    User systemUser = getOrCreateSystemUser();
                    Message systemMessage = new Message();
                    systemMessage.setConversationId(conversationId);
                    systemMessage.setSender(systemUser);
                    systemMessage.setReceiver(conversation.getCustomer());
                    systemMessage.setContent("Bạn đã nhắn tin trong thời gian chờ. Nhân viên sẽ tiếp tục hỗ trợ bạn nhé! 😊");
                    systemMessage.setIsAiGenerated(false);
                    systemMessage.setSentAt(LocalDateTime.now());
                    Message savedMessage = messageRepository.save(systemMessage);
                    
                    // Gửi message qua WebSocket cho customer
                    com.example.demo.dto.ChatMessageDTO messageDTO = new com.example.demo.dto.ChatMessageDTO();
                    messageDTO.setId(savedMessage.getId());
                    messageDTO.setConversationId(conversationId);
                    messageDTO.setSenderId(systemUser.getId());
                    messageDTO.setSenderName("Hệ thống");
                    messageDTO.setContent(savedMessage.getContent());
                    messageDTO.setSentAt(savedMessage.getSentAt());
                    messageDTO.setIsAiGenerated(false);
                    
                    webSocketService.sendChatMessage(messageDTO);
                    webSocketService.sendConversationUpdate(conversationId, "system_notice", null);
                }
            } catch (Exception e) {
                log.error("Error sending cancel notice", e);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Task định kỳ xử lý các conversation đã hết hạn chờ
     * Trao lại cho AI nếu khách không nhắn tin trong 30 giây
     * Chạy mỗi 2 giây
     */
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processPendingReturns() {
        if (aiResumePending.isEmpty()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        aiResumePending.entrySet().removeIf(entry -> {
            Long conversationId = entry.getKey();
            LocalDateTime deadline = entry.getValue();
            
            // Kiểm tra nếu đã hết hạn
            if (now.isAfter(deadline)) {
                try {
                    returnConversationToAi(conversationId);
                    log.info("✅ Processed AI return for conversation {}", conversationId);
                    return true; // Remove from queue
                } catch (Exception e) {
                    log.error("Error processing AI return for conversation {}: {}", conversationId, e.getMessage(), e);
                    return true; // Remove anyway to prevent stuck
                }
            }
            
            return false; // Keep in queue
        });
    }
    
    /**
     * Thực sự trao conversation lại cho AI
     */
    @Transactional
    public void returnConversationToAi(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        
        if (conversation == null) {
            log.warn("Conversation {} not found when returning to AI", conversationId);
            return;
        }
        
        if (conversation.getAssignedStaff() == null) {
            log.warn("Conversation {} already has no assigned staff", conversationId);
            return;
        }
        
        // Unassign staff và đổi về OPEN để AI có thể xử lý
        conversation.setAssignedStaff(null);
        conversation.setStatus(ConversationStatus.OPEN);
        conversationRepository.save(conversation);
        
        log.info("🤖 Returned conversation {} to AI (unassigned staff)", conversationId);
        
        // Gửi system message + lời chào của AI
        User aiUser = getOrCreateAiUser();
        
        // System message
        User systemUser = getOrCreateSystemUser();
        Message systemMessage = new Message();
        systemMessage.setConversationId(conversationId);
        systemMessage.setSender(systemUser);
        systemMessage.setReceiver(conversation.getCustomer());
        systemMessage.setContent("Hoa AI đã tiếp tục hỗ trợ bạn.");
        systemMessage.setIsAiGenerated(false);
        systemMessage.setSentAt(LocalDateTime.now());
        Message savedSystemMessage = messageRepository.save(systemMessage);
        
        // Gửi system message qua WebSocket
        com.example.demo.dto.ChatMessageDTO systemMessageDTO = new com.example.demo.dto.ChatMessageDTO();
        systemMessageDTO.setId(savedSystemMessage.getId());
        systemMessageDTO.setConversationId(conversationId);
        systemMessageDTO.setSenderId(systemUser.getId());
        systemMessageDTO.setSenderName("Hệ thống");
        systemMessageDTO.setContent(savedSystemMessage.getContent());
        systemMessageDTO.setSentAt(savedSystemMessage.getSentAt());
        systemMessageDTO.setIsAiGenerated(false);
        webSocketService.sendChatMessage(systemMessageDTO);
        
        // AI greeting
        Message aiGreeting = new Message();
        aiGreeting.setConversationId(conversationId);
        aiGreeting.setSender(aiUser);
        aiGreeting.setReceiver(conversation.getCustomer());
        aiGreeting.setContent("Chào bạn! Mình là Hoa AI, sẵn sàng tiếp tục tư vấn cho bạn nè. Bạn cứ mô tả mong muốn, mình tư vấn liền nhé! 🌸✨");
        aiGreeting.setIsAiGenerated(true);
        aiGreeting.setSentAt(LocalDateTime.now());
        Message savedAiGreeting = messageRepository.save(aiGreeting);
        
        // Gửi AI greeting qua WebSocket
        com.example.demo.dto.ChatMessageDTO aiMessageDTO = new com.example.demo.dto.ChatMessageDTO();
        aiMessageDTO.setId(savedAiGreeting.getId());
        aiMessageDTO.setConversationId(conversationId);
        aiMessageDTO.setSenderId(aiUser.getId());
        aiMessageDTO.setSenderName("Hoa AI 🌸");
        aiMessageDTO.setContent(savedAiGreeting.getContent());
        aiMessageDTO.setSentAt(savedAiGreeting.getSentAt());
        aiMessageDTO.setIsAiGenerated(true);
        webSocketService.sendChatMessage(aiMessageDTO);
        
        // Gửi conversation update
        webSocketService.sendConversationUpdate(conversationId, "status_change", null);
        
        log.info("✅ AI greeting sent to conversation {}", conversationId);
    }
    
    /**
     * Get or create AI user
     */
    private User getOrCreateAiUser() {
        return userRepository.findByEmail("ai@system.local").orElseGet(() -> {
            log.info("Creating AI system user");
            User aiUser = new User();
            aiUser.setEmail("ai@system.local");
            aiUser.setFirstname("Hoa AI");
            aiUser.setLastname("🌸");
            aiUser.setPassword(passwordEncoder.encode("ai_system_password_" + System.currentTimeMillis()));
            aiUser.setPhone("0000000000");
            aiUser.setRole(UserRole.STAFF);
            return userRepository.save(aiUser);
        });
    }
    
    /**
     * Get or create system user
     */
    private User getOrCreateSystemUser() {
        return userRepository.findByEmail("system@local").orElseGet(() -> {
            log.info("Creating system user");
            User systemUser = new User();
            systemUser.setEmail("system@local");
            systemUser.setFirstname("Hệ thống");
            systemUser.setLastname("StarShop");
            systemUser.setPassword(passwordEncoder.encode("system_password_" + System.currentTimeMillis()));
            systemUser.setPhone("0000000001");
            systemUser.setRole(UserRole.STAFF);
            return userRepository.save(systemUser);
        });
    }
}
