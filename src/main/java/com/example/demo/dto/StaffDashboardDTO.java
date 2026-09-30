package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for staff dashboard statistics and information
 * Aggregates data from multiple sources for dashboard display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardDTO {
    
    // Staff information
    private Long staffId;
    private String staffName;
    private String staffCode;
    private String position;
    
    // Current shift information
    private StaffCheckInDTO currentShift;
    
    // Conversation statistics
    private Long activeConversations;
    private Long assignedConversations;
    private Long unreadMessages;
    private Long todayMessages;
    
    // Work statistics
    private BigDecimal todayHoursWorked;
    private BigDecimal weekHoursWorked;
    private BigDecimal monthHoursWorked;
    
    // Recent conversations
    private List<ConversationDTO> recentConversations = new ArrayList<>();
    
    // Unassigned conversations queue
    private List<ConversationDTO> unassignedQueue = new ArrayList<>();
    
    // Performance metrics
    private Long totalResolvedToday;
    private Double avgResponseTimeMinutes;
    
    // Helper methods
    public boolean isOnShift() {
        return currentShift != null && currentShift.isActiveShift();
    }
    
    public String getShiftStatus() {
        if (currentShift == null) {
            return "Chưa check-in";
        }
        return currentShift.getShiftStatus();
    }
    
    public boolean hasUnreadMessages() {
        return unreadMessages != null && unreadMessages > 0;
    }
    
    public boolean hasActiveConversations() {
        return activeConversations != null && activeConversations > 0;
    }
}

