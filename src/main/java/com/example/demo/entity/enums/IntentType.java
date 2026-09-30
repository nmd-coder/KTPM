package com.example.demo.entity.enums;

/**
 * Intent types detected by AI for customer messages
 * Used in routing decisions to categorize customer inquiries
 */
public enum IntentType {
    SALES("Tư vấn sản phẩm"),
    SHIPPING("Phí vận chuyển"),
    PROMOTION("Khuyến mãi"),
    ORDER_SUPPORT("Hỗ trợ đơn hàng"),
    PAYMENT("Thanh toán"),
    STORE_INFO("Thông tin cửa hàng"),
    CHITCHAT("Trò chuyện"),
    OTHER("Khác");

    private final String displayName;

    IntentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static IntentType fromString(String value) {
        if (value == null) {
            return OTHER;
        }
        try {
            return IntentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}

