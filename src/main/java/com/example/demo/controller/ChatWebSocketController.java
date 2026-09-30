package com.example.demo.controller;

import com.example.demo.dto.ChatMessageDTO;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ChatService;
import com.example.demo.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket Controller for Real-time Chat
 * Handles WebSocket STOMP messages for chat functionality
 * Following rules.mdc specifications for WebSocket with STOMP
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    /**
     * Handle sending chat messages via WebSocket
     * Endpoint: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO message, Principal principal) {
        try {
            log.info("Received chat message via WebSocket from user: {}, conversation: {}", 
                principal != null ? principal.getName() : "unknown", 
                message.getConversationId());
            
            // IMPORTANT: Ensure senderId is set from authenticated user
            // If senderId is null, try to get it from the authenticated principal
            if (message.getSenderId() == null) {
                log.warn("SenderId is null in message payload, attempting to get from authenticated principal");
                
                if (principal != null && principal.getName() != null) {
                    try {
                        String userEmail = principal.getName();
                        log.info("Looking up user by email: {}", userEmail);
                        
                        User authenticatedUser = userRepository.findByEmail(userEmail).orElse(null);
                        if (authenticatedUser != null) {
                            // Guardrail: avoid resolving staff as system users
                            if ("system@local".equalsIgnoreCase(authenticatedUser.getEmail()) ||
                                "ai@system.local".equalsIgnoreCase(authenticatedUser.getEmail())) {
                                log.error("CRITICAL: Authenticated principal resolved to a system user ({}). Blocking send.", authenticatedUser.getEmail());
                                throw new RuntimeException("Authenticated user resolved to system user");
                            }
                            message.setSenderId(authenticatedUser.getId());
                            log.info("Set senderId from authenticated user: {}", authenticatedUser.getId());
                        } else {
                            log.error("Authenticated user not found in database: {}", userEmail);
                            throw new RuntimeException("Authenticated user not found");
                        }
                    } catch (Exception e) {
                        log.error("Error looking up authenticated user", e);
                        throw new RuntimeException("SenderId is required and could not be determined from authentication");
                    }
                } else {
                    log.error("No principal found and senderId is null");
                    throw new RuntimeException("SenderId is required and no authenticated user found");
                }
            } else {
                log.info("Message senderId from payload: {}", message.getSenderId());
            }
            
            // Validate that we now have a senderId
            if (message.getSenderId() == null) {
                throw new RuntimeException("SenderId is still null after attempting to set from authentication");
            }
            
            // Send message through chat service
            log.info("🚀 Calling chatService.sendMessage() at {}", System.currentTimeMillis());
            ChatMessageDTO sentMessage = chatService.sendMessage(message);
            
            // WebSocket service will broadcast to conversation participants
            log.info("✅ Chat message processed successfully, ID: {}, SenderName: {}, broadcasted at {}", 
                sentMessage.getId(), 
                sentMessage.getSenderName(),
                System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("Error processing chat message via WebSocket", e);
        }
    }

    /**
     * Handle typing indicator
     * Endpoint: /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> typingData, Principal principal) {
        try {
            // Validate input data first
            if (typingData == null) {
                log.warn("Typing data is null");
                return;
            }
            
            // Handle both String and Number types for conversationId and userId
            Object conversationIdObj = typingData.get("conversationId");
            Object userIdObj = typingData.get("userId");
            
            // Early validation for null objects
            if (conversationIdObj == null) {
                log.warn("ConversationId is null in typing data: {}", typingData);
                return;
            }
            
            if (userIdObj == null) {
                log.warn("UserId is null in typing data: {}", typingData);
                return;
            }
            
            Long conversationId;
            Long userId;
            
            // Parse conversationId with proper error handling
            try {
                if (conversationIdObj instanceof String) {
                    conversationId = Long.parseLong((String) conversationIdObj);
                } else if (conversationIdObj instanceof Number) {
                    conversationId = ((Number) conversationIdObj).longValue();
                } else {
                    log.error("Invalid conversationId type: {}, value: {}", 
                        conversationIdObj.getClass().getSimpleName(), conversationIdObj);
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("Cannot parse conversationId: {}", conversationIdObj, e);
                return;
            }
            
            // Parse userId with proper error handling
            try {
                if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                } else if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else {
                    log.error("Invalid userId type: {}, value: {}", 
                        userIdObj.getClass().getSimpleName(), userIdObj);
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("Cannot parse userId: {}", userIdObj, e);
                return;
            }
            
            String userName = (String) typingData.get("userName");
            if (userName == null) {
                userName = "Unknown User";
            }
            
            log.debug("Processing typing indicator - conversationId: {}, userId: {}, userName: {}", 
                conversationId, userId, userName);
            
            // Broadcast typing indicator to conversation
            webSocketService.sendTypingIndicator(conversationId.toString(), userId, userName);
            
        } catch (Exception e) {
            log.error("Error handling typing indicator", e);
        }
    }

    /**
     * Handle user joining a conversation
     * Endpoint: /app/chat.join
     */
    @MessageMapping("/chat.join")
    public void joinConversation(@Payload Map<String, Object> joinData, 
                                  SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long conversationId = ((Number) joinData.get("conversationId")).longValue();
            Long userId = ((Number) joinData.get("userId")).longValue();
            String userName = (String) joinData.get("userName");
            
            log.info("User {} ({}) joined conversation {}", userName, userId, conversationId);
            
            // Store user info in WebSocket session
            var sessionAttributes = headerAccessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("conversationId", conversationId);
                sessionAttributes.put("userId", userId);
                sessionAttributes.put("userName", userName);
            }
            
            // Send system message to conversation
            String systemMessage = userName + " đã tham gia cuộc hội thoại";
            chatService.sendSystemMessage(conversationId, systemMessage);
            
        } catch (Exception e) {
            log.error("Error handling user join", e);
        }
    }

    /**
     * Handle user leaving a conversation
     * Endpoint: /app/chat.leave
     */
    @MessageMapping("/chat.leave")
    public void leaveConversation(@Payload Map<String, Object> leaveData) {
        try {
            Long conversationId = ((Number) leaveData.get("conversationId")).longValue();
            Long userId = ((Number) leaveData.get("userId")).longValue();
            String userName = (String) leaveData.get("userName");
            
            log.info("User {} ({}) left conversation {}", userName, userId, conversationId);
            
            // Send system message to conversation
            String systemMessage = userName + " đã rời khỏi cuộc hội thoại";
            chatService.sendSystemMessage(conversationId, systemMessage);
            
        } catch (Exception e) {
            log.error("Error handling user leave", e);
        }
    }

    /**
     * Handle marking messages as read
     * Endpoint: /app/chat.read
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, Object> readData) {
        try {
            Long conversationId = ((Number) readData.get("conversationId")).longValue();
            Long userId = ((Number) readData.get("userId")).longValue();
            
            chatService.markMessagesAsRead(conversationId, userId);
            
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
        }
    }
}
