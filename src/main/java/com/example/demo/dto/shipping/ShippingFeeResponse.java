package com.example.demo.dto.shipping;

import java.math.BigDecimal;

public record ShippingFeeResponse(
    BigDecimal shippingFee,
    Integer serviceTypeIdUsed,
    String message,
    boolean success
) {
    public static ShippingFeeResponse success(BigDecimal fee, Integer serviceTypeId) {
        return new ShippingFeeResponse(fee, serviceTypeId, "Tính phí thành công", true);
    }
    
    public static ShippingFeeResponse error(String message) {
        return new ShippingFeeResponse(BigDecimal.ZERO, null, message, false);
    }
}
