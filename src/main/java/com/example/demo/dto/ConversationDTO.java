package com.example.demo.dto;

import com.example.demo.entity.enums.ConversationPriority;
import com.example.demo.entity.enums.ConversationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for conversation information
 * Includes customer and staff details for display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    
    private Long id;
    private ConversationStatus status;
    private ConversationPriority priority;
    
    // Customer information
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerAvatar;
    private String customerPhone;
    
    // Assigned staff information
    private Long assignedStaffId;
    private String assignedStaffName;
    private String assignedStaffCode;
    private String assignedStaffAvatar;
    
    // Last message info
    private String lastMessageContent;
    private Long lastMessageSenderId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime closedAt;
    
    // Statistics
    private Long unreadCount;
    private Long totalMessageCount;
    
    // Internal notes
    private String notes;
    
    // AI Handoff information
    private String handoffReason;
    private String aiContext;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enqueuedAt;
    
    private Integer waitTimeSeconds;
    
    // Helper methods
    public boolean isUnassigned() {
        return status == ConversationStatus.OPEN;
    }
    
    public boolean isActive() {
        return status == ConversationStatus.OPEN || status == ConversationStatus.ASSIGNED;
    }
    
    public boolean isClosed() {
        return status == ConversationStatus.CLOSED;
    }
    
    public String getWaitTimeFormatted() {
        if (waitTimeSeconds == null) {
            return "N/A";
        }
        long minutes = waitTimeSeconds / 60;
        long seconds = waitTimeSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}

