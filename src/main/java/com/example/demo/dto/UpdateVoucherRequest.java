package com.example.demo.dto;

import com.example.demo.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoucherRequest {
    
    @Size(max = 255, message = "Tên voucher không được vượt quá 255 ký tự")
    private String name;
    
    private String description;
    
    private DiscountType discountType;
    
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.0", message = "Giảm giá tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxDiscountAmount;
    
    private LocalDate expiryDate;
    
    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal minOrderValue;
    
    @Min(value = 1, message = "Số lượt sử dụng tối đa phải lớn hơn 0")
    private Integer maxUses;
    
    private Boolean isActive;
}

