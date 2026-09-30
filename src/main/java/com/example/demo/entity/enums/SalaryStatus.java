package com.example.demo.entity.enums;

public enum SalaryStatus {
    PENDING("pending"),
    PAID("paid"),
    OVERDUE("overdue");

    private final String value;

    SalaryStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static SalaryStatus fromValue(String value) {
        for (SalaryStatus status : SalaryStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SalaryStatus: " + value);
    }
}
