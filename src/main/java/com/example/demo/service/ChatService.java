package com.example.demo.service;

import com.example.demo.client.GeminiClient;
import com.example.demo.dto.ChatMessageDTO;
import com.example.demo.dto.ConversationDTO;
import com.example.demo.entity.Conversation;
import com.example.demo.entity.Message;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.ConversationStatus;
import com.example.demo.entity.enums.MessageType;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing chat conversations and messages
 * Following rules.mdc specifications for business logic tier
 */
@Service
@Slf4j
@Transactional
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final PasswordEncoder passwordEncoder;
    
    // AI Integration services (optional - graceful degradation if not available)
    private final RoutingService routingService;
    private final AiChatService aiChatService;
    private final AiToolExecutorService aiToolExecutorService;
    private final HandoffService handoffService;
    private final StoreConfigService storeConfigService;
    private final ConversationSupervisorService conversationSupervisorService;
    
    // SSE streaming emitters for real-time AI responses
    private final Map<Long, SseEmitter> streamingEmitters = new ConcurrentHashMap<>();
    
    // Constructor with @Lazy for ConversationSupervisorService to break circular dependency
    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            WebSocketService webSocketService,
            PasswordEncoder passwordEncoder,
            RoutingService routingService,
            AiChatService aiChatService,
            AiToolExecutorService aiToolExecutorService,
            HandoffService handoffService,
            StoreConfigService storeConfigService,
            @Lazy ConversationSupervisorService conversationSupervisorService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
        this.passwordEncoder = passwordEncoder;
        this.routingService = routingService;
        this.aiChatService = aiChatService;
        this.aiToolExecutorService = aiToolExecutorService;
        this.handoffService = handoffService;
        this.storeConfigService = storeConfigService;
        this.conversationSupervisorService = conversationSupervisorService;
    }

    /**
     * Start a new conversation for a customer
     * If customer already has an open conversation, return that instead
     */
    public ConversationDTO startConversation(Long customerId) {
        log.info("Starting conversation for customer ID: {}", customerId);
        
        User customer = userRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Only allow customers to start conversations
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new RuntimeException("Only customers can start conversations");
        }
        
        // Check if customer already has an open or assigned conversation
        Optional<Conversation> existingConv = conversationRepository
            .findByCustomerIdAndStatusIn(customerId, 
                Arrays.asList(ConversationStatus.OPEN, ConversationStatus.ASSIGNED));
        
        if (existingConv.isPresent()) {
            log.info("Found existing conversation ID: {}", existingConv.get().getId());
            return convertToDTO(existingConv.get());
        }
        
        // Create new conversation
        Conversation conversation = new Conversation(customer);
        conversation = conversationRepository.save(conversation);
        
        log.info("Created new conversation ID: {}", conversation.getId());
        
        // Notify staff about new conversation
        webSocketService.sendNotification(null, 
            "Cuộc hội thoại mới từ " + customer.getFullName(), 
            "new_conversation");
        
        return convertToDTO(conversation);
    }

    /**
     * Assign conversation to a specific staff member
     */
    public ConversationDTO assignConversation(Long conversationId, Long staffId) {
        log.info("Assigning conversation {} to staff {}", conversationId, staffId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        User staff = userRepository.findById(staffId)
            .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        if (staff.getRole() != UserRole.STAFF && staff.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("User is not a staff member");
        }
        
        conversation.assignToStaff(staff);
        conversation = conversationRepository.save(conversation);
        
        // Send system message
        String systemMessage = "Cuộc hội thoại đã được chuyển đến " + staff.getFullName();
        sendSystemMessage(conversation.getId(), systemMessage);
        
        // Notify staff
        webSocketService.sendNotification(staffId, 
            "Bạn đã được giao cuộc hội thoại với " + conversation.getCustomer().getFullName(), 
            "conversation_assigned");
        
        // CRITICAL: Notify customer so they can subscribe to conversation topic
        // This ensures customer receives real-time messages from staff
        Long customerId = conversation.getCustomer().getId();
        Map<String, Object> customerNotificationData = new HashMap<>();
        customerNotificationData.put("conversationId", conversation.getId());
        customerNotificationData.put("staffId", staffId);
        customerNotificationData.put("staffName", staff.getFullName());
        
        // Send to customer's personal queue
        webSocketService.sendPersonalChatUpdate(customerId, "conversation_assigned", customerNotificationData);
        log.info("📢 Sent conversation_assigned notification to customer {} for conversation {}", 
            customerId, conversation.getId());
        
        return convertToDTO(conversation);
    }

    /**
     * Auto-assign conversation to available staff with least workload
     */
    public ConversationDTO autoAssignConversation(Long conversationId) {
        log.info("Auto-assigning conversation {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        // Find staff with least active conversations
        List<User> staffList = userRepository.findByRole(UserRole.STAFF);
        
        if (staffList.isEmpty()) {
            log.warn("No staff available for auto-assignment");
            return convertToDTO(conversation);
        }
        
        User selectedStaff = null;
        Long minWorkload = Long.MAX_VALUE;
        
        for (User staff : staffList) {
            if (staff.getIsActive()) {
                Long workload = conversationRepository.getStaffActiveConversationCount(staff.getId());
                if (workload < minWorkload) {
                    minWorkload = workload;
                    selectedStaff = staff;
                }
            }
        }
        
        if (selectedStaff != null) {
            return assignConversation(conversationId, selectedStaff.getId());
        }
        
        return convertToDTO(conversation);
    }

    /**
     * Send a message in a conversation
     */
    public ChatMessageDTO sendMessage(ChatMessageDTO messageDTO) {
        log.debug("Sending message in conversation: {}", messageDTO.getConversationId());
        
        // Validate required fields
        if (messageDTO.getSenderId() == null) {
            throw new RuntimeException("SenderId is required");
        }
        
        if (messageDTO.getConversationId() == null) {
            throw new RuntimeException("ConversationId is required");
        }
        
        User sender = userRepository.findById(messageDTO.getSenderId())
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        log.info("🔍 DEBUG: Sender loaded from DB - ID: {}, Email: {}, FirstName: {}, LastName: {}, FullName: {}", 
            sender.getId(), 
            sender.getEmail(), 
            sender.getFirstname(), 
            sender.getLastname(), 
            sender.getFullName());
        
        Conversation conversation = conversationRepository.findById(messageDTO.getConversationId())
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        // ============ STAFF MESSAGE HANDLING ============
        // If staff/admin sends message, assign conversation to them and set status to ASSIGNED
        if (sender.getRole() == UserRole.STAFF || sender.getRole() == UserRole.ADMIN) {
            log.info("Staff/Admin message detected, assigning conversation {} to staff {}", 
                conversation.getId(), sender.getId());
            
            // Assign conversation to staff if not already assigned to them
            if (conversation.getAssignedStaff() == null || 
                !conversation.getAssignedStaff().getId().equals(sender.getId())) {
                conversation.assignToStaff(sender);
                conversationRepository.save(conversation);
                log.info("Conversation {} assigned to staff {}", conversation.getId(), sender.getId());
            }
        }
        
        // Determine receiver based on conversation
        User receiver = null;
        if (messageDTO.getReceiverId() != null) {
            // Only call findById if receiverId is not null
            receiver = userRepository.findById(messageDTO.getReceiverId())
                .orElse(null);
        }
        
        // If no receiver found yet, find the other participant in the conversation
        if (receiver == null) {
            if (sender.getId().equals(conversation.getCustomer().getId())) {
                // Customer is sending, receiver is assigned staff (if any). If none, route to system user
                receiver = conversation.getAssignedStaff();
                if (receiver == null) {
                    // Ensure receiver_id is set for unassigned conversations
                    receiver = getOrCreateSystemUser();
                }
            } else {
                // Staff is sending, receiver is customer
                receiver = conversation.getCustomer();
            }
        }
        
        // Create message entity
        Message message = new Message(sender, receiver, messageDTO.getContent());
        message.setConversationId(messageDTO.getConversationId());
        message.setMessageType(messageDTO.getMessageType() != null ? 
            messageDTO.getMessageType() : MessageType.TEXT);
        message.setIsAiGenerated(messageDTO.getIsAiGenerated() != null ? 
            messageDTO.getIsAiGenerated() : false);
        
        Message savedMessage = messageRepository.save(message);
        
        // Update conversation last message time
        conversation.setLastMessageAt(savedMessage.getSentAt());
        conversationRepository.save(conversation);
        
        // Convert to DTO and send via WebSocket
        ChatMessageDTO resultDTO = convertMessageToDTO(savedMessage);
        webSocketService.sendChatMessage(resultDTO);
        
        // Send conversation update to staff
        webSocketService.sendConversationUpdate(messageDTO.getConversationId(), "new_message", resultDTO);
        
        // Send general chat update (like Messenger/Zalo)
        webSocketService.sendChatUpdate("message_sent", resultDTO);
        
        // ============ CANCEL AI RETURN IF PENDING ============
        // If customer sends message while staff is waiting to return to AI, cancel the return
        if (sender.getRole() == UserRole.CUSTOMER && conversation.getAssignedStaff() != null) {
            boolean canceled = conversationSupervisorService.cancelIfPendingByCustomerMessage(conversation.getId());
            if (canceled) {
                log.info("Cancelled AI return for conversation {} due to customer message", conversation.getId());
                // Continue with staff handling (don't do AI routing)
                return resultDTO;
            }
        }
        
        // ============ AI ROUTING FOR CUSTOMER MESSAGES ============
        // CRITICAL FIX: Only allow AI routing if conversation is OPEN status (not ASSIGNED)
        // If message is from customer and conversation is OPEN (no staff assigned), try AI routing
        if (sender.getRole() == UserRole.CUSTOMER) {
            // CRITICAL FIX: Re-fetch conversation to get latest status and prevent race condition
            // This ensures we see the latest status even if staff just assigned the conversation
            Conversation latestConversation = conversationRepository.findById(conversation.getId())
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            if (latestConversation.getStatus() == ConversationStatus.OPEN && latestConversation.getAssignedStaff() == null) {
                log.info("Customer message in OPEN conversation detected, attempting AI routing for conversation {}", latestConversation.getId());
                
                try {
                    // Route message through AI
                    RoutingService.RoutingDecision decision = routingService.routeMessage(
                        conversation.getId(), 
                        messageDTO.getContent()
                    );
                    
                    if (decision.isHandleByAi()) {
                        // AI can handle this message
                        log.debug("AI handling message for conversation {}", conversation.getId());
                        
                        com.example.demo.dto.AiAnalysisResult analysis = decision.getAiAnalysis();
                        
                        // Execute tools if requested and generate smart response
                        String aiReply;
                        if (analysis.hasToolRequests()) {
                            log.info("Executing tools and generating context-aware response");
                            String toolResults = aiToolExecutorService.executeTools(analysis);
                            
                            // Check if streaming is available for this conversation
                            boolean hasStreaming = streamingEmitters.containsKey(conversation.getId());
                            
                            if (hasStreaming) {
                                // Generate with streaming support
                                aiReply = aiChatService.generateFinalResponse(
                                    conversation.getId(), 
                                    messageDTO.getContent(),
                                    toolResults,
                                    analysis,
                                    new GeminiClient.StreamingCallback() {
                                        @Override
                                        public void onChunk(String chunk) {
                                            sendStreamingChunk(conversation.getId(), chunk);
                                        }
                                        
                                        @Override
                                        public void onComplete() {
                                            // Will be handled after message is saved
                                        }
                                        
                                        @Override
                                        public void onError(String error) {
                                            sendStreamingError(conversation.getId(), error);
                                        }
                                        
                                        @Override
                                        public void onStreamingUnavailable() {
                                            log.warn("Streaming unavailable for conversation {}", conversation.getId());
                                        }
                                    }
                                );
                            } else {
                                // Generate without streaming
                                aiReply = aiChatService.generateFinalResponse(
                                    conversation.getId(), 
                                    messageDTO.getContent(),
                                    toolResults,
                                    analysis
                                );
                            }
                        } else {
                            // No tools needed, use initial reply
                            aiReply = analysis.getReply();
                        }
                        
                        // Add handoff suggestion if needed
                        if (decision.isSuggestHandoff()) {
                            aiReply += "\n\n_Bạn có muốn mình chuyển cho nhân viên để được hỗ trợ tốt hơn không?_ 💬";
                        }
                        
                        // Create AI response message
                        Message aiMessage = new Message();
                        // Get or create AI system user
                        User aiUser = getOrCreateAiUser();
                        aiMessage.setSender(aiUser);
                        aiMessage.setReceiver(sender);
                        aiMessage.setContent(aiReply);
                        aiMessage.setConversationId(conversation.getId());
                        aiMessage.setMessageType(MessageType.TEXT);
                        aiMessage.setIsAiGenerated(true);
                        
                        Message savedAiMessage = messageRepository.save(aiMessage);
                        
                        // Update conversation
                        conversation.setLastMessageAt(savedAiMessage.getSentAt());
                        conversationRepository.save(conversation);
                        
                        // Send AI response via WebSocket
                        ChatMessageDTO aiMessageDTO = convertMessageToDTO(savedAiMessage);
                        aiMessageDTO.setSenderName("Hoa AI 🌸");
                        webSocketService.sendChatMessage(aiMessageDTO);
                        
                        // Send streaming completion if streaming was used
                        if (streamingEmitters.containsKey(conversation.getId())) {
                            sendStreamingComplete(conversation.getId(), aiMessageDTO);
                        }
                        
                        log.debug("AI response sent for conversation {}", conversation.getId());
                        
                    } else {
                        // Need to handoff to staff
                        log.info("Handing off conversation {} to staff - Reason: {}", 
                            conversation.getId(), decision.getHandoffReason());
                        
                        // Add to handoff queue
                        handoffService.addToQueue(
                            conversation.getId(),
                            decision.getHandoffReason(),
                            messageDTO.getContent(),
                            decision.getContext(),
                            null // priority will be determined by reason
                        );
                        
                        // Send system message to customer
                        String handoffMessage = decision.getContext() != null ? 
                            decision.getContext() : 
                            "Mình xin phép chuyển bạn cho nhân viên để được hỗ trợ tốt hơn nhé 💬";
                        
                        Message systemMessage = new Message();
                        User systemUser = getOrCreateSystemUser();
                        systemMessage.setSender(systemUser);
                        systemMessage.setReceiver(sender);
                        systemMessage.setContent(handoffMessage);
                        systemMessage.setConversationId(conversation.getId());
                        systemMessage.setMessageType(MessageType.SYSTEM);
                        systemMessage.setIsAiGenerated(true);
                        
                        Message savedSystemMessage = messageRepository.save(systemMessage);
                        
                        // Send system message via WebSocket
                        ChatMessageDTO systemMessageDTO = convertMessageToDTO(savedSystemMessage);
                        systemMessageDTO.setSenderName("Hệ thống");
                        webSocketService.sendChatMessage(systemMessageDTO);
                        
                        // Note: HandoffService.addToQueue() already sends staff notifications
                    }
                    
                } catch (Exception e) {
                    log.error("Error in AI routing", e);
                    // Continue without AI - message already sent
                }
            } else {
                // CRITICAL FIX: Conversation was just assigned by staff (race condition detected)
                log.info("Race condition detected: Conversation {} status changed to {} or staff assigned during message processing. Skipping AI routing.", 
                    latestConversation.getId(), latestConversation.getStatus());
                // Close any pending streaming emitters
                if (streamingEmitters.containsKey(latestConversation.getId())) {
                    sendStreamingComplete(latestConversation.getId(), null);
                }
            }
        }
        
        log.info("Message sent successfully, ID: {}", savedMessage.getId());
        return resultDTO;
    }

    /**
     * Send first message and create conversation if needed
     * This method handles the case where a customer sends their first message
     * NOW WITH AI INTEGRATION
     */
    public ChatMessageDTO sendFirstMessage(ChatMessageDTO messageDTO) {
        log.info("Sending first message for customer: {}", messageDTO.getSenderId());
        
        User sender = userRepository.findById(messageDTO.getSenderId())
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        // Only customers can send first messages
        if (sender.getRole() != UserRole.CUSTOMER) {
            throw new RuntimeException("Only customers can send first messages");
        }
        
        // Check if customer already has an open conversation
        Optional<Conversation> existingConv = conversationRepository
            .findByCustomerIdAndStatusIn(sender.getId(), 
                Arrays.asList(ConversationStatus.OPEN, ConversationStatus.ASSIGNED));
        
        Conversation conversation;
        boolean isNewConversation = false;
        
        if (existingConv.isPresent()) {
            conversation = existingConv.get();
            log.info("Using existing conversation ID: {}", conversation.getId());
        } else {
            // Create new conversation
            conversation = new Conversation(sender);
            conversation = conversationRepository.save(conversation);
            isNewConversation = true;
            log.info("Created new conversation ID: {}", conversation.getId());
        }
        
        // Save customer message
        User initialReceiver = conversation.getAssignedStaff();
        if (initialReceiver == null) {
            // When no staff assigned yet, send to system user to avoid NULL receiver_id
            initialReceiver = getOrCreateSystemUser();
        }
        Message customerMessage = new Message(sender, initialReceiver, messageDTO.getContent());
        customerMessage.setConversationId(conversation.getId());
        customerMessage.setMessageType(messageDTO.getMessageType() != null ? 
            messageDTO.getMessageType() : MessageType.TEXT);
        customerMessage.setIsAiGenerated(false);
        
        Message savedCustomerMessage = messageRepository.save(customerMessage);
        
        // Update conversation last message time
        conversation.setLastMessageAt(savedCustomerMessage.getSentAt());
        conversationRepository.save(conversation);
        
        // Send customer message via WebSocket
        ChatMessageDTO customerMessageDTO = convertMessageToDTO(savedCustomerMessage);
        webSocketService.sendChatMessage(customerMessageDTO);
        
        // ============ AI ROUTING LOGIC ============
        try {
            // CRITICAL FIX: Add delay to allow frontend WebSocket subscription to complete
            // This prevents race condition where AI response is broadcast before client subscribes
            // Increased to 400ms to ensure subscription completes even on slow networks
            if (isNewConversation) {
                try {
                    Thread.sleep(400); // 400ms delay for new conversations
                    log.info("⏱️ Delayed AI routing for new conversation {} to allow WebSocket subscription", conversation.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Delay interrupted for conversation {}", conversation.getId());
                }
            }
            
            // Route message through AI
            RoutingService.RoutingDecision decision = routingService.routeMessage(
                conversation.getId(), 
                messageDTO.getContent()
            );
            
            if (decision.isHandleByAi()) {
                // AI can handle this message
                log.debug("AI handling message for conversation {}", conversation.getId());
                
                com.example.demo.dto.AiAnalysisResult analysis = decision.getAiAnalysis();
                
                // Execute tools if requested and generate smart response
                String aiReply;
                if (analysis.hasToolRequests()) {
                    log.info("Executing tools and generating context-aware response");
                    String toolResults = aiToolExecutorService.executeTools(analysis);
                    
                    // Generate final smart response based on tool results
                    aiReply = aiChatService.generateFinalResponse(
                        conversation.getId(), 
                        messageDTO.getContent(),
                        toolResults,
                        analysis
                    );
                } else {
                    // No tools needed, use initial reply
                    aiReply = analysis.getReply();
                }
                
                // Add handoff suggestion if needed
                if (decision.isSuggestHandoff()) {
                    aiReply += "\n\n_Bạn có muốn mình chuyển cho nhân viên để được hỗ trợ tốt hơn không?_ 💬";
                }
                
                // Create AI response message
                Message aiMessage = new Message();
                User aiUser = getOrCreateAiUser();
                aiMessage.setSender(aiUser);
                aiMessage.setReceiver(sender);
                aiMessage.setContent(aiReply);
                aiMessage.setConversationId(conversation.getId());
                aiMessage.setMessageType(MessageType.TEXT);
                aiMessage.setIsAiGenerated(true);
                
                Message savedAiMessage = messageRepository.save(aiMessage);
                
                // Update conversation
                conversation.setLastMessageAt(savedAiMessage.getSentAt());
                conversationRepository.save(conversation);
                
                // Send AI response via WebSocket
                ChatMessageDTO aiMessageDTO = convertMessageToDTO(savedAiMessage);
                aiMessageDTO.setSenderName("Hoa AI 🌸");
                webSocketService.sendChatMessage(aiMessageDTO);
                
                log.debug("AI response sent for conversation {}", conversation.getId());
                
            } else {
                // Need to handoff to staff
                log.info("Handing off conversation {} to staff - Reason: {}", 
                    conversation.getId(), decision.getHandoffReason());
                
                // Add to handoff queue
                handoffService.addToQueue(
                    conversation.getId(),
                    decision.getHandoffReason(),
                    messageDTO.getContent(),
                    decision.getContext(),
                    null // priority will be determined by reason
                );
                
                // Send system message to customer
                String handoffMessage = decision.getContext() != null ? 
                    decision.getContext() : 
                    "Mình xin phép chuyển bạn cho nhân viên để được hỗ trợ tốt hơn nhé 💬";
                
                Message systemMessage = new Message();
                User systemUser = getOrCreateSystemUser();
                systemMessage.setSender(systemUser);
                systemMessage.setReceiver(sender);
                systemMessage.setContent(handoffMessage);
                systemMessage.setConversationId(conversation.getId());
                systemMessage.setMessageType(MessageType.SYSTEM);
                systemMessage.setIsAiGenerated(true);
                
                Message savedSystemMessage = messageRepository.save(systemMessage);
                
                // Send system message via WebSocket
                ChatMessageDTO systemMessageDTO = convertMessageToDTO(savedSystemMessage);
                systemMessageDTO.setSenderName("Hệ thống");
                webSocketService.sendChatMessage(systemMessageDTO);
                
                // Note: HandoffService.addToQueue() already sends staff notifications
            }
            
        } catch (Exception e) {
            log.error("Error in AI routing, falling back to staff handoff", e);
            
            // Fallback: add to handoff queue
            try {
                handoffService.addToQueue(
                    conversation.getId(),
                    com.example.demo.entity.enums.HandoffReason.AI_ERROR,
                    messageDTO.getContent(),
                    "Lỗi hệ thống AI",
                    5 // medium priority
                );
                // Note: HandoffService.addToQueue() already sends staff notifications
                
            } catch (Exception fallbackError) {
                log.error("Error in fallback handoff", fallbackError);
            }
        }
        
        // Send conversation update to staff
        webSocketService.sendConversationUpdate(conversation.getId(), "new_message", customerMessageDTO);
        
        // Send general chat update (like Messenger/Zalo)
        webSocketService.sendChatUpdate("message_sent", customerMessageDTO);
        
        log.info("First message sent successfully");
        return customerMessageDTO;
    }

    /**
     * Send a system message (not from user)
     */
    public void sendSystemMessage(Long conversationId, String content) {
        ChatMessageDTO systemMessage = ChatMessageDTO.createSystemMessage(conversationId, content);
        webSocketService.sendChatMessage(systemMessage);
    }

    /**
     * Close a conversation
     */
    public ConversationDTO closeConversation(Long conversationId) {
        log.info("Closing conversation {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.close();
        conversation = conversationRepository.save(conversation);
        
        // Send system message
        sendSystemMessage(conversationId, "Cuộc hội thoại đã được đóng");
        
        return convertToDTO(conversation);
    }

    /**
     * Reopen a closed conversation
     */
    public ConversationDTO reopenConversation(Long conversationId) {
        log.info("Reopening conversation {}", conversationId);
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        conversation.reopen();
        conversation = conversationRepository.save(conversation);
        
        // Send system message
        sendSystemMessage(conversationId, "Cuộc hội thoại đã được mở lại");
        
        return convertToDTO(conversation);
    }

    /**
     * Get all conversations for a staff member
     */
    @Transactional(readOnly = true)
    public List<ConversationDTO> getStaffConversations(Long staffId, int page, int size) {
        log.info("Getting conversations for staff {}", staffId);
        
        Pageable pageable = PageRequest.of(page, size);
        // Only get ASSIGNED conversations for this staff
        // OPEN conversations have no assignedStaffId, so they won't match this query
        Page<Conversation> conversations = conversationRepository
            .findByAssignedStaffIdAndStatusInOrderByLastMessageAtDesc(
                staffId, 
                Arrays.asList(ConversationStatus.ASSIGNED),
                pageable);
        
        return conversations.stream()
            .map(conv -> convertToStaffDTO(conv, staffId))
            .collect(Collectors.toList());
    }

    /**
     * Get unassigned conversations (queue)
     */
    @Transactional(readOnly = true)
    public List<ConversationDTO> getUnassignedConversations(int page, int size) {
        log.info("Getting unassigned conversations");
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = conversationRepository
            .findUnassignedConversations(pageable);
        
        return conversations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get messages for a conversation
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getConversationMessages(String conversationId, int page, int size) {
        log.info("Getting messages for conversation {}", conversationId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository
            .findByConversationIdOrderBySentAtDesc(Long.parseLong(conversationId), pageable);
        
        return messages.stream()
            .map(this::convertMessageToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Mark messages as read in a conversation
     */
    public void markMessagesAsRead(Long conversationId, Long userId) {
        log.info("Marking messages as read for conversation {} and user {}", conversationId, userId);
        messageRepository.markConversationMessagesAsRead(conversationId, userId);
    }

    /**
     * Get conversation by ID
     */
    @Transactional(readOnly = true)
    public ConversationDTO getConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return convertToDTO(conversation);
    }

    /**
     * Get customer's conversations
     */
    @Transactional(readOnly = true)
    public List<ConversationDTO> getCustomerConversations(Long customerId) {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Conversation> conversations = conversationRepository
            .findByCustomerIdOrderByLastMessageAtDesc(customerId, pageable);
        
        return conversations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Search staff conversations
     */
    @Transactional(readOnly = true)
    public List<ConversationDTO> searchStaffConversations(Long staffId, String searchTerm, int page, int size) {
        log.info("Searching conversations for staff {} with term: {}", staffId, searchTerm);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversations = conversationRepository
            .searchStaffConversations(staffId, searchTerm, 
                Arrays.asList(ConversationStatus.ASSIGNED, ConversationStatus.OPEN),
                pageable);
        
        return conversations.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // Helper methods

    private ConversationDTO convertToStaffDTO(Conversation conversation, Long staffId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setStatus(conversation.getStatus());
        dto.setPriority(conversation.getPriority());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setClosedAt(conversation.getClosedAt());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setNotes(conversation.getNotes());
        
        // Customer info
        User customer = conversation.getCustomer();
        dto.setCustomerId(customer.getId());
        dto.setCustomerName(customer.getFullName());
        dto.setCustomerEmail(customer.getEmail());
        dto.setCustomerAvatar(customer.getAvatar());
        dto.setCustomerPhone(customer.getPhone());
        
        // Staff info
        if (conversation.getAssignedStaff() != null) {
            User staff = conversation.getAssignedStaff();
            dto.setAssignedStaffId(staff.getId());
            dto.setAssignedStaffName(staff.getFullName());
            dto.setAssignedStaffCode(staff.getEmployeeCode());
            dto.setAssignedStaffAvatar(staff.getAvatar());
        }
        
        // Last message info
        List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversation.getId(), PageRequest.of(0, 1));
        if (!lastMessages.isEmpty()) {
            Message lastMessage = lastMessages.get(0);
            dto.setLastMessageContent(lastMessage.getContent());
            dto.setLastMessageSenderId(lastMessage.getSender().getId());
        }
        
        // Unread count for staff - count unread messages from customer to staff
        Long unreadCount = messageRepository.countUnreadByConversationIdAndReceiverId(conversation.getId(), staffId);
        dto.setUnreadCount(unreadCount);
        
        return dto;
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setStatus(conversation.getStatus());
        dto.setPriority(conversation.getPriority());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setClosedAt(conversation.getClosedAt());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setNotes(conversation.getNotes());
        
        // Customer info
        User customer = conversation.getCustomer();
        dto.setCustomerId(customer.getId());
        dto.setCustomerName(customer.getFullName());
        dto.setCustomerEmail(customer.getEmail());
        dto.setCustomerAvatar(customer.getAvatar());
        dto.setCustomerPhone(customer.getPhone());
        
        // Staff info
        if (conversation.getAssignedStaff() != null) {
            User staff = conversation.getAssignedStaff();
            dto.setAssignedStaffId(staff.getId());
            dto.setAssignedStaffName(staff.getFullName());
            dto.setAssignedStaffCode(staff.getEmployeeCode());
            dto.setAssignedStaffAvatar(staff.getAvatar());
        }
        
        // Last message info
        List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversation.getId(), PageRequest.of(0, 1));
        if (!lastMessages.isEmpty()) {
            Message lastMessage = lastMessages.get(0);
            dto.setLastMessageContent(lastMessage.getContent());
            dto.setLastMessageSenderId(lastMessage.getSender().getId());
        }
        
        // Unread count - count unread messages in this conversation for the customer
        Long unreadCount = messageRepository.countUnreadByConversationIdAndReceiverId(conversation.getId(), customer.getId());
        dto.setUnreadCount(unreadCount);
        
        return dto;
    }

    private ChatMessageDTO convertMessageToDTO(Message message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFullName());
        dto.setSenderAvatar(message.getSender().getAvatar());
        
        log.debug("🔄 Converting message to DTO - MessageID: {}, SenderID: {}, SenderName: {}", 
            message.getId(), 
            message.getSender().getId(), 
            message.getSender().getFullName());
        
        // FIXED: Set receiverId correctly - receiver is different from sender
        if (message.getReceiver() != null) {
            dto.setReceiverId(message.getReceiver().getId());
            dto.setReceiverName(message.getReceiver().getFullName());
        } else {
            // If no explicit receiver, infer from conversation context
            // This helps with WebSocket routing
            dto.setReceiverId(null);
        }
        
        dto.setConversationId(message.getConversationId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setIsRead(message.getIsRead());
        dto.setIsAiGenerated(message.getIsAiGenerated());
        dto.setSentAt(message.getSentAt());
        return dto;
    }

    /**
     * Get or create AI system user
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
     * Get or create System user
     */
    private User getOrCreateSystemUser() {
        return userRepository.findByEmail("system@local").orElseGet(() -> {
            log.info("Creating System user");
            User systemUser = new User();
            systemUser.setEmail("system@local");
            systemUser.setFirstname("Hệ");
            systemUser.setLastname("thống");
            systemUser.setPassword(passwordEncoder.encode("system_password_" + System.currentTimeMillis()));
            systemUser.setPhone("0000000001");
            systemUser.setRole(UserRole.STAFF);
            return userRepository.save(systemUser);
        });
    }
    
    /**
     * Register SSE emitter for streaming AI responses
     */
    public void registerStreamingEmitter(Long conversationId, SseEmitter emitter) {
        log.info("Registering SSE emitter for conversation {}", conversationId);
        streamingEmitters.put(conversationId, emitter);
    }
    
    /**
     * Unregister SSE emitter for conversation
     */
    public void unregisterStreamingEmitter(Long conversationId) {
        log.info("Unregistering SSE emitter for conversation {}", conversationId);
        streamingEmitters.remove(conversationId);
    }
    
    /**
     * Send streaming chunk to conversation
     */
    public void sendStreamingChunk(Long conversationId, String content) {
        SseEmitter emitter = streamingEmitters.get(conversationId);
        if (emitter != null) {
            try {
                String jsonData = String.format("{\"type\":\"chunk\",\"content\":\"%s\"}", 
                    content.replace("\"", "\\\"").replace("\n", "\\n"));
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(jsonData));
                log.debug("Sent streaming chunk to conversation {}: {} chars", conversationId, content.length());
            } catch (IOException e) {
                log.error("Failed to send streaming chunk to conversation {}: {}", conversationId, e.getMessage());
                streamingEmitters.remove(conversationId);
            }
        }
    }
    
    /**
     * Send streaming completion to conversation
     */
    public void sendStreamingComplete(Long conversationId, ChatMessageDTO finalMessage) {
        SseEmitter emitter = streamingEmitters.get(conversationId);
        if (emitter != null) {
            try {
                String jsonData = String.format("{\"type\":\"complete\",\"finalMessage\":%s}", 
                    finalMessage != null ? finalMessage.toJson() : "null");
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(jsonData));
                log.info("Sent streaming completion to conversation {}", conversationId);
                
                // Close the emitter after completion
                emitter.complete();
                streamingEmitters.remove(conversationId);
            } catch (IOException e) {
                log.error("Failed to send streaming completion to conversation {}: {}", conversationId, e.getMessage());
                streamingEmitters.remove(conversationId);
            }
        }
    }
    
    /**
     * Send streaming error to conversation
     */
    public void sendStreamingError(Long conversationId, String errorMessage) {
        SseEmitter emitter = streamingEmitters.get(conversationId);
        if (emitter != null) {
            try {
                String jsonData = String.format("{\"type\":\"error\",\"message\":\"%s\"}", 
                    errorMessage.replace("\"", "\\\""));
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(jsonData));
                log.error("Sent streaming error to conversation {}: {}", conversationId, errorMessage);
                
                // Close the emitter after error
                emitter.complete();
                streamingEmitters.remove(conversationId);
            } catch (IOException e) {
                log.error("Failed to send streaming error to conversation {}: {}", conversationId, e.getMessage());
                streamingEmitters.remove(conversationId);
            }
        }
    }
}
