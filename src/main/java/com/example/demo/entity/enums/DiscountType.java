package com.example.demo.entity.enums;

public enum DiscountType {
    PERCENTAGE("percentage"),
    FIXED("fixed");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
