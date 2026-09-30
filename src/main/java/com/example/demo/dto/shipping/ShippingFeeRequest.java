package com.example.demo.dto.shipping;

public record ShippingFeeRequest(
    Long addressId,
    Integer serviceTypeId,
    Integer overrideWeight,
    Integer overrideLength,
    Integer overrideWidth,
    Integer overrideHeight
) {}
