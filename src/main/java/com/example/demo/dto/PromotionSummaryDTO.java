package com.example.demo.dto;

import com.example.demo.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for promotion/voucher summary for AI responses
 * Contains essential voucher information for chat display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionSummaryDTO {
    
    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal discountValue;
    private DiscountType discountType;
    private BigDecimal minOrderValue;
    private LocalDate expiryDate;
    private Integer remainingUses;
    private Boolean isActive;

    /**
     * Format discount as readable text
     */
    public String getFormattedDiscount() {
        if (discountType == DiscountType.PERCENTAGE) {
            return String.format("Giảm %,.0f%%", discountValue);
        } else {
            return String.format("Giảm %,.0fđ", discountValue);
        }
    }

    /**
     * Format minimum order value
     */
    public String getFormattedMinOrder() {
        if (minOrderValue == null || minOrderValue.compareTo(BigDecimal.ZERO) == 0) {
            return "Không yêu cầu tối thiểu";
        }
        return String.format("Đơn tối thiểu %,.0fđ", minOrderValue);
    }

    /**
     * Get expiry date as readable text
     */
    public String getExpiryText() {
        if (expiryDate == null) {
            return "Không giới hạn";
        }
        return "Hết hạn: " + expiryDate.toString();
    }

    /**
     * Check if voucher is still valid
     */
    public boolean isValid() {
        return isActive && (expiryDate == null || expiryDate.isAfter(LocalDate.now()));
    }
}

