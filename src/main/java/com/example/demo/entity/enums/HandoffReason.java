package com.example.demo.entity.enums;

/**
 * Reasons why a conversation was handed off from AI to staff
 * Used in handoff queue and routing decisions
 */
public enum HandoffReason {
    LOW_CONFIDENCE("AI không tự tin với câu trả lời"),
    PII_DETECTED("Phát hiện thông tin cá nhân"),
    ORDER_INQUIRY("Câu hỏi về đơn hàng"),
    PAYMENT_ISSUE("Vấn đề thanh toán"),
    EXPLICIT_REQUEST("Khách yêu cầu nói chuyện với nhân viên"),
    AI_ERROR("Lỗi hệ thống AI"),
    COMPLEX_QUERY("Câu hỏi phức tạp");

    private final String displayName;

    HandoffReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static HandoffReason fromString(String value) {
        if (value == null) {
            return COMPLEX_QUERY;
        }
        try {
            return HandoffReason.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMPLEX_QUERY;
        }
    }
}

