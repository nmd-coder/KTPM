package com.example.demo.entity.enums;

/**
 * Staff presence status
 * Used in staff_presence table
 */
public enum StaffStatus {
    AVAILABLE("Sẵn sàng"),
    BUSY("Bận"),
    AWAY("Vắng mặt"),
    OFFLINE("Offline");

    private final String displayName;

    StaffStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StaffStatus fromString(String value) {
        if (value == null) {
            return OFFLINE;
        }
        try {
            return StaffStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OFFLINE;
        }
    }
}

