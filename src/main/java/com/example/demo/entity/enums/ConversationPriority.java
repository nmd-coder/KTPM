package com.example.demo.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Conversation Priority Enum
 * Represents the urgency level of a customer conversation
 */
public enum ConversationPriority {
    LOW("LOW"),
    NORMAL("NORMAL"),
    HIGH("HIGH"),
    URGENT("URGENT");

    private final String value;

    ConversationPriority(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        switch (this) {
            case LOW: return "Thấp";
            case NORMAL: return "Bình thường";
            case HIGH: return "Cao";
            case URGENT: return "Khẩn cấp";
            default: return "Bình thường";
        }
    }
    
    public String getColorClass() {
        switch (this) {
            case LOW: return "text-gray-500";
            case NORMAL: return "text-blue-500";
            case HIGH: return "text-orange-500";
            case URGENT: return "text-red-500";
            default: return "text-blue-500";
        }
    }

    @JsonCreator
    public static ConversationPriority fromString(String value) {
        if (value == null) {
            return NORMAL; // default value
        }
        
        // Try direct enum name match (case insensitive)
        for (ConversationPriority priority : ConversationPriority.values()) {
            if (priority.name().equalsIgnoreCase(value)) {
                return priority;
            }
        }
        
        // Try value match (case insensitive)
        for (ConversationPriority priority : ConversationPriority.values()) {
            if (priority.value.equalsIgnoreCase(value)) {
                return priority;
            }
        }
        
        // Default fallback
        return NORMAL;
    }

    @Override
    public String toString() {
        return value;
    }
}

