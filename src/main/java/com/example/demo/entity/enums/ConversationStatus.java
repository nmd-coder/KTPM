package com.example.demo.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Conversation Status Enum
 * Represents the current state of a conversation between customer and staff
 */
public enum ConversationStatus {
    OPEN("OPEN"),           // New conversation, not yet assigned to any staff
    ASSIGNED("ASSIGNED"),   // Conversation assigned to a staff member
    CLOSED("CLOSED");       // Conversation has been resolved and closed

    private final String value;

    ConversationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        switch (this) {
            case OPEN: return "Chưa xử lý";
            case ASSIGNED: return "Đang xử lý";
            case CLOSED: return "Đã đóng";
            default: return "Không xác định";
        }
    }

    @JsonCreator
    public static ConversationStatus fromString(String value) {
        if (value == null) {
            return OPEN; // default value
        }
        
        // Try direct enum name match (case insensitive)
        for (ConversationStatus status : ConversationStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        // Try value match (case insensitive)
        for (ConversationStatus status : ConversationStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        
        // Default fallback
        return OPEN;
    }

    @Override
    public String toString() {
        return value;
    }
}

