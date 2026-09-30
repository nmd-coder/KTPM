package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for shipping fee estimate from AI
 * Contains estimated shipping cost and delivery time
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeEstimate {
    
    private BigDecimal fee;
    private String estimatedTime;
    private String fromLocation;
    private String toLocation;
    private Integer serviceTypeId;
    private Boolean success;
    private String errorMessage;

    /**
     * Create successful estimate
     */
    public static ShippingFeeEstimate success(BigDecimal fee, String estimatedTime) {
        ShippingFeeEstimate estimate = new ShippingFeeEstimate();
        estimate.setFee(fee);
        estimate.setEstimatedTime(estimatedTime);
        estimate.setSuccess(true);
        return estimate;
    }

    /**
     * Create error estimate
     */
    public static ShippingFeeEstimate error(String errorMessage) {
        ShippingFeeEstimate estimate = new ShippingFeeEstimate();
        estimate.setSuccess(false);
        estimate.setErrorMessage(errorMessage);
        estimate.setFee(BigDecimal.ZERO);
        return estimate;
    }

    /**
     * Format fee as Vietnamese currency
     */
    public String getFormattedFee() {
        if (fee == null) {
            return "0đ";
        }
        return String.format("%,.0fđ", fee);
    }

    /**
     * Get readable estimate text
     */
    public String getEstimateText() {
        if (!success) {
            return errorMessage != null ? errorMessage : "Không thể tính phí ship";
        }
        
        StringBuilder text = new StringBuilder();
        text.append("Phí ship: ").append(getFormattedFee());
        
        if (estimatedTime != null && !estimatedTime.isEmpty()) {
            text.append(" - Thời gian: ").append(estimatedTime);
        }
        
        return text.toString();
    }
}

