package com.example.demo.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    CUSTOMER("customer"),
    STAFF("staff"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
    
    public String getDisplayName() {
        switch (this) {
            case CUSTOMER: return "Khách hàng";
            case STAFF: return "Nhân viên";
            case ADMIN: return "Quản trị viên";
            default: return "Khách hàng";
        }
    }

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return CUSTOMER; // default value
        }
        
        // Try direct enum name match (case insensitive)
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        
        // Try value match (case insensitive)
        for (UserRole role : UserRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        
        // If no match found, try to match by name only
        try {
            return UserRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOMER; // default fallback
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
