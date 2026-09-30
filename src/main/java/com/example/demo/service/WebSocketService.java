
package com.example.demo.service;

import com.example.demo.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket Service for sending real-time messages
 * Following rules.mdc specifications for WebSocket messaging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send welcome message to user after successful login
     * @param userId User ID
     * @param userName User's full name
     */
    public void sendWelcomeMessage(Long userId, String userName) {
        if (userId == null) {
            log.warn("Cannot send welcome message: userId is null");
            return;
        }
        
        try {
            String welcomeMessage = String.format(
                "🌸 Welcome back to StarShop, %s! Happy shopping! 🌹", 
                userName != null ? userName : "Dear Customer"
            );
            
            // Send to user-specific topic as per rules.mdc: /topic/messages/{userId}
            String destination = "/topic/messages/" + userId;
            
            WelcomeMessagePayload payload = new WelcomeMessagePayload(
                "welcome",
                welcomeMessage,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            messagingTemplate.convertAndSend(destination, payload);
            
            log.info("Welcome message sent to user {} at destination: {}", userId, destination);
            
        } catch (Exception e) {
            log.error("Error sending welcome message to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Send order status update message
     * @param userId User ID
     * @param orderId Order ID
     * @param status New order status
     */
    public void sendOrderStatusUpdate(Long userId, String orderId, String status) {
        if (userId == null || orderId == null) {
            log.warn("Cannot send order update: userId or orderId is null");
            return;
        }
        
        try {
            String message = String.format(
                "📦 Your order #%d status has been updated to: %s", 
                orderId, status
            );
            
            OrderUpdatePayload payload = new OrderUpdatePayload(
                "order_update",
                message,
                orderId,
                status,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            // Send to user-specific topic
            String destination = "/topic/messages/" + userId;
            messagingTemplate.convertAndSend(destination, payload);
            
            // Also send to general orders topic
            messagingTemplate.convertAndSend("/topic/orders", payload);
            
            log.info("Order status update sent to user {} for order {}: {}", userId, orderId, status);
            
        } catch (Exception e) {
            log.error("Error sending order status update to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Send general notification to user
     * @param userId User ID
     * @param message Notification message
     * @param type Message type
     */
    public void sendNotification(Long userId, String message, String type) {
        if (userId == null || message == null) {
            log.warn("Cannot send notification: userId or message is null");
            return;
        }
        
        try {
            NotificationPayload payload = new NotificationPayload(
                type != null ? type : "notification",
                message,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            String destination = "/topic/messages/" + userId;
            messagingTemplate.convertAndSend(destination, payload);
            
            log.info("Notification sent to user {}: {}", userId, message);
            
        } catch (Exception e) {
            log.error("Error sending notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Send broadcast message to all staff members
     * @param message Broadcast message
     * @param type Message type
     */
    public void sendBroadcast(String message, String type) {
        sendBroadcast(message, type, null);
    }
    
    /**
     * Send broadcast message to all staff members with conversation context
     * @param message Broadcast message
     * @param type Message type
     * @param conversationId Related conversation ID (optional)
     */
    public void sendBroadcast(String message, String type, Long conversationId) {
        if (message == null) {
            log.warn("Cannot send broadcast: message is null");
            return;
        }
        
        try {
            // Create notification with conversation context
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", type != null ? type : "broadcast");
            payload.put("message", message);
            payload.put("conversationId", conversationId);
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // ENHANCED: Add priority level for critical errors
            if ("critical_error".equals(type)) {
                payload.put("priority", "critical");
                payload.put("requiresAcknowledgment", true);
                
                // Also send to dedicated critical alerts topic
                messagingTemplate.convertAndSend("/topic/critical-alerts", payload);
                log.error("🚨 CRITICAL ALERT sent to staff: {}", message);
            }
            
            // Send to staff notifications topic
            messagingTemplate.convertAndSend("/topic/staff/notifications", payload);
            
            log.info("Broadcast notification sent to staff: {} (conversation: {})", message, conversationId);
            
        } catch (Exception e) {
            log.error("Error sending broadcast notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send conversation update to staff
     * @param conversationId Conversation ID
     * @param updateType Type of update (new_message, status_change, etc.)
     * @param data Update data (can be ChatMessageDTO or null)
     */
    public void sendConversationUpdate(Long conversationId, String updateType, Object data) {
        if (conversationId == null) {
            log.warn("Cannot send conversation update: conversationId is null");
            return;
        }
        
        try {
            ConversationUpdatePayload payload = new ConversationUpdatePayload(
                conversationId,
                updateType,
                data,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            messagingTemplate.convertAndSend("/topic/conversation-updates", payload);
            
            log.info("Conversation update sent for conversation {}: {}", conversationId, updateType);
            
        } catch (Exception e) {
            log.error("Error sending conversation update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Overloaded method for ChatMessageDTO specifically
     * @param conversationId Conversation ID
     * @param updateType Type of update
     * @param chatMessage Chat message data
     */
    public void sendConversationUpdate(Long conversationId, String updateType, ChatMessageDTO chatMessage) {
        sendConversationUpdate(conversationId, updateType, (Object) chatMessage);
    }

    /**
     * Send general chat update (like Messenger/Zalo)
     * @param updateType Type of update
     * @param data Update data
     */
    public void sendChatUpdate(String updateType, Object data) {
        if (updateType == null) {
            log.warn("Cannot send chat update: updateType is null");
            return;
        }
        
        try {
            ChatUpdatePayload payload = new ChatUpdatePayload(
                updateType,
                data,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            messagingTemplate.convertAndSend("/topic/chat-updates", payload);
            
            log.info("Chat update sent: {}", updateType);
            
        } catch (Exception e) {
            log.error("Error sending chat update: {}", e.getMessage(), e);
        }
    }

    /**
     * Send personal chat update to specific user
     * @param userId User ID to send to
     * @param updateType Type of update
     * @param data Update data
     */
    public void sendPersonalChatUpdate(Long userId, String updateType, Object data) {
        if (userId == null || updateType == null) {
            log.warn("Cannot send personal chat update: userId or updateType is null");
            return;
        }
        
        try {
            ChatUpdatePayload payload = new ChatUpdatePayload(
                updateType,
                data,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            String destination = "/user/" + userId.toString() + "/queue/chat-updates";
            log.info("📤 Sending personal chat update to destination: {}", destination);
            log.info("📤 Update type: {}, Data: {}", updateType, data);
            
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/chat-updates",
                payload
            );
            
            log.info("✅ Personal chat update sent to user {}: {}", userId, updateType);
            
        } catch (Exception e) {
            log.error("❌ Error sending personal chat update to user {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Send chat message to conversation participants
     * SIMPLIFIED ROUTING: Only send to conversation topic to ensure all participants receive messages
     * @param chatMessage Chat message DTO
     */
    public void sendChatMessage(ChatMessageDTO chatMessage) {
        if (chatMessage == null || chatMessage.getConversationId() == null) {
            log.warn("Cannot send chat message: message or conversationId is null");
            return;
        }
        
        try {
            Long conversationId = chatMessage.getConversationId();
            
            // PRIMARY & ONLY: Send to conversation topic
            // Both customer and staff subscribe to /topic/chat/{conversationId}
            // This ensures ALL participants in the conversation receive the message
            String conversationTopic = "/topic/chat/" + conversationId;
            messagingTemplate.convertAndSend(conversationTopic, chatMessage);
            
            log.info("📤 Chat message sent to conversation topic {}: {} (AI: {}, Sender: {} [{}], Receiver: {})", 
                conversationTopic,
                chatMessage.getContent().substring(0, Math.min(50, chatMessage.getContent().length())), 
                chatMessage.getIsAiGenerated(),
                chatMessage.getSenderId(),
                chatMessage.getSenderName(),
                chatMessage.getReceiverId());
            
            // OPTIONAL: Broadcast to chat-updates for additional notification purposes
            // This is NOT for displaying messages, only for triggering UI updates
            try {
                java.util.Map<String, Object> updateData = new java.util.HashMap<>();
                updateData.put("conversationId", conversationId);
                updateData.put("message", chatMessage);
                sendChatUpdate("message_sent", updateData);
            } catch (Exception e) {
                log.warn("Failed to broadcast chat update for conversation {}", conversationId, e);
            }
            
        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage(), e);
        }
    }

    /**
     * Notify staff about new message
     * @param staffId Staff ID
     * @param chatMessage Chat message
     */
    public void notifyStaffNewMessage(Long staffId, ChatMessageDTO chatMessage) {
        if (staffId == null) {
            log.warn("Cannot notify staff: staffId is null");
            return;
        }
        
        try {
            String notification = String.format(
                "Tin nhắn mới từ %s trong cuộc hội thoại #%s",
                chatMessage.getSenderName(),
                chatMessage.getConversationId()
            );
            
            NotificationPayload payload = new NotificationPayload(
                "new_message",
                notification,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            String destination = "/topic/messages/" + staffId;
            messagingTemplate.convertAndSend(destination, payload);
            
            log.info("New message notification sent to staff {}", staffId);
            
        } catch (Exception e) {
            log.error("Error notifying staff {}: {}", staffId, e.getMessage(), e);
        }
    }

    /**
     * Notify staff about new handoff in queue
     */
    public void notifyStaffNewHandoff(Long conversationId) {
        try {
            NotificationPayload payload = new NotificationPayload(
                "new_handoff",
                "Có cuộc hội thoại mới cần hỗ trợ từ AI",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            payload.setConversationId(conversationId);
            
            messagingTemplate.convertAndSend("/topic/handoff-queue", payload);
            log.info("New handoff notification sent for conversation {}", conversationId);
            
        } catch (Exception e) {
            log.error("Error notifying staff about new handoff", e);
        }
    }

    /**
     * Notify specific staff about conversation assignment
     */
    public void notifyStaffAssignment(Long staffId, Long conversationId) {
        try {
            NotificationPayload payload = new NotificationPayload(
                "conversation_assigned",
                "Bạn đã được gán cuộc hội thoại mới",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            payload.setConversationId(conversationId);
            
            messagingTemplate.convertAndSendToUser(
                staffId.toString(),
                "/queue/assignments",
                payload
            );
            
            log.info("Assignment notification sent to staff {} for conversation {}", 
                staffId, conversationId);
            
        } catch (Exception e) {
            log.error("Error notifying staff about assignment", e);
        }
    }

    /**
     * Broadcast to all staff members
     * @param message Broadcast message
     */
    public void broadcastToStaff(String message) {
        if (message == null) {
            log.warn("Cannot broadcast to staff: message is null");
            return;
        }
        
        try {
            NotificationPayload payload = new NotificationPayload(
                "staff_broadcast",
                message,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            messagingTemplate.convertAndSend("/topic/staff/notifications", payload);
            
            log.info("Broadcast message sent to all staff: {}", message);
            
        } catch (Exception e) {
            log.error("Error broadcasting to staff: {}", e.getMessage(), e);
        }
    }

    /**
     * Send typing indicator
     * @param conversationId Conversation ID
     * @param userId User ID who is typing
     * @param userName User name who is typing
     */
    public void sendTypingIndicator(String conversationId, Long userId, String userName) {
        if (conversationId == null || userId == null) {
            return;
        }
        
        try {
            TypingIndicatorPayload payload = new TypingIndicatorPayload(
                userId,
                userName,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            String destination = "/topic/chat/" + conversationId + "/typing";
            messagingTemplate.convertAndSend(destination, payload);
            
        } catch (Exception e) {
            log.error("Error sending typing indicator: {}", e.getMessage(), e);
        }
    }

    // Message payload classes
    public static class WelcomeMessagePayload {
        public final String type;
        public final String message;
        public final String timestamp;
        
        public WelcomeMessagePayload(String type, String message, String timestamp) {
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
    
    public static class OrderUpdatePayload {
        public final String type;
        public final String message;
        public final String orderId;
        public final String status;
        public final String timestamp;
        
        public OrderUpdatePayload(String type, String message, String orderId, String status, String timestamp) {
            this.type = type;
            this.message = message;
            this.orderId = orderId;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
    
    public static class NotificationPayload {
        public final String type;
        public final String message;
        public final String timestamp;
        private Long conversationId;
        
        public NotificationPayload(String type, String message, String timestamp) {
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public Long getConversationId() {
            return conversationId;
        }
        
        public void setConversationId(Long conversationId) {
            this.conversationId = conversationId;
        }
    }
    
    public static class TypingIndicatorPayload {
        public final Long userId;
        public final String userName;
        public final String timestamp;
        
        public TypingIndicatorPayload(Long userId, String userName, String timestamp) {
            this.userId = userId;
            this.userName = userName;
            this.timestamp = timestamp;
        }
    }
    
    public static class ConversationUpdatePayload {
        public final Long conversationId;
        public final String updateType;
        public final Object data;
        public final String timestamp;
        
        public ConversationUpdatePayload(Long conversationId, String updateType, Object data, String timestamp) {
            this.conversationId = conversationId;
            this.updateType = updateType;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
    
    public static class ChatUpdatePayload {
        public final String type;
        public final Object data;
        public final String timestamp;
        
        public ChatUpdatePayload(String type, Object data, String timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
