package com.example.demo.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Message Type Enum
 * Distinguishes between different types of messages in the chat system
 */
public enum MessageType {
    TEXT("TEXT"),               // Regular text message from user
    SYSTEM("SYSTEM"),           // System-generated message (e.g., "Conversation assigned to staff")
    AI_RESPONSE("AI_RESPONSE"); // AI-generated response (for future Gemini integration)

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        switch (this) {
            case TEXT: return "Tin nhắn";
            case SYSTEM: return "Hệ thống";
            case AI_RESPONSE: return "AI trả lời";
            default: return "Tin nhắn";
        }
    }

    @JsonCreator
    public static MessageType fromString(String value) {
        if (value == null) {
            return TEXT; // default value
        }
        
        // Try direct enum name match (case insensitive)
        for (MessageType type : MessageType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        // Try value match (case insensitive)
        for (MessageType type : MessageType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        // Default fallback
        return TEXT;
    }

    @Override
    public String toString() {
        return value;
    }
}

