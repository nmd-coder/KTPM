package com.example.demo.entity.enums;

/**
 * Payment method enumeration
 * Following rules.mdc specifications for enum usage
 */
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng", "Cash on Delivery"),
    MOMO("Ví điện tử MoMo", "MoMo Wallet"),
    BANK_TRANSFER("Chuyển khoản ngân hàng", "Bank Transfer"),
    CREDIT_CARD("Thẻ tín dụng", "Credit Card");
    
    private final String displayName;
    private final String englishName;
    
    PaymentMethod(String displayName, String englishName) {
        this.displayName = displayName;
        this.englishName = englishName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    /**
     * Check if payment method is available
     * @return true if available, false otherwise
     */
    public boolean isAvailable() {
        switch (this) {
            case COD:
                return true;
            case MOMO:
                return true; // Đã bật sandbox
            case BANK_TRANSFER:
                return false; // Chưa implement
            case CREDIT_CARD:
                return false; // Chưa implement
            default:
                return false;
        }
    }
    
    /**
     * Get status message for payment method
     * @return Status message
     */
    public String getStatusMessage() {
        switch (this) {
            case COD:
                return "Sẵn sàng";
            case MOMO:
                return "Sẵn sàng";
            case BANK_TRANSFER:
                return "Chưa hỗ trợ";
            case CREDIT_CARD:
                return "Chưa hỗ trợ";
            default:
                return "Không xác định";
        }
    }
}