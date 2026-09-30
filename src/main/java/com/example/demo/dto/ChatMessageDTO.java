package com.example.demo.dto;

import com.example.demo.entity.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for chat messages
 * Used for transferring message data between client and server
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private String receiverName;
    private Long conversationId;
    private String content;
    private MessageType messageType;
    private Boolean isRead;
    private Boolean isAiGenerated;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;
    
    // Constructor for sending new messages
    public ChatMessageDTO(Long senderId, Long receiverId, Long conversationId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.conversationId = conversationId;
        this.content = content;
        this.messageType = MessageType.TEXT;
        this.isRead = false;
        this.isAiGenerated = false;
        this.sentAt = LocalDateTime.now();
    }
    
    // Constructor for system messages
    public static ChatMessageDTO createSystemMessage(Long conversationId, String content) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setConversationId(conversationId);
        dto.setContent(content);
        dto.setMessageType(MessageType.SYSTEM);
        dto.setIsRead(false);
        dto.setIsAiGenerated(false);
        dto.setSentAt(LocalDateTime.now());
        return dto;
    }
    
    /**
     * Convert to JSON string for streaming
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Failed to serialize message\"}";
        }
    }
}

