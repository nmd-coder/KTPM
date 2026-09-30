package com.example.demo.entity.enums;

public enum OrderStatus {
    PENDING("Chờ xử lý", "Pending"),
    PROCESSING("Đang xử lý", "Processing"),
    SHIPPED("Đang giao hàng", "Shipped"),
    COMPLETED("Hoàn thành", "Completed"),
    RECEIVED("Đã nhận hàng", "Received"),
    CANCELLED("Đã hủy", "Cancelled");

    private final String displayName;
    private final String englishName;

    OrderStatus(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEnglishName() {
        return englishName;
    }
}
