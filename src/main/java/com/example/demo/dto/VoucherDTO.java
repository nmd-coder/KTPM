package com.example.demo.dto;

import com.example.demo.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private LocalDate expiryDate;
    private Integer maxUses;
    private Integer uses;
    private Boolean isActive;
    private LocalDateTime createdAt;
    
    // Calculated fields
    private String status;
    private Double usagePercentage;
    private Long timesUsedInOrders;
    private BigDecimal totalOrderValue;
    
    public String getDiscountTypeDisplay() {
        if (discountType == null) return "";
        return discountType == DiscountType.PERCENTAGE ? "Giảm theo %" : "Giảm cố định";
    }
    
    public String getStatusDisplay() {
        if (status == null) return "Không xác định";
        switch (status) {
            case "ACTIVE": return "Hoạt động";
            case "EXPIRED": return "Hết hạn";
            case "USED_UP": return "Đã hết lượt";
            case "INACTIVE": return "Tạm dừng";
            default: return status;
        }
    }
    
    public boolean isValid() {
        return "ACTIVE".equals(status);
    }
}

