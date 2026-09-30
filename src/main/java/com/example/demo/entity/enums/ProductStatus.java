package com.example.demo.entity.enums;

/**
 * Product status enumeration
 */
public enum ProductStatus {
    ACTIVE("active", "Đang bán"),
    INACTIVE("inactive", "Tạm ngưng"),
    OUT_OF_STOCK("out_of_stock", "Hết hàng"),
    DISCONTINUED("discontinued", "Ngừng kinh doanh");

    private final String value;
    private final String displayName;

    ProductStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return value;
    }
}
