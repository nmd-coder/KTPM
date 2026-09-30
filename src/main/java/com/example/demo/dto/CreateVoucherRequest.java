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
public class CreateVoucherRequest {
    
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Mã voucher chỉ được chứa chữ in hoa, số và dấu gạch ngang")
    private String code;
    
    @NotBlank(message = "Tên voucher không được để trống")
    @Size(max = 255, message = "Tên voucher không được vượt quá 255 ký tự")
    private String name;
    
    private String description;
    
    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;
    
    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.0", message = "Giảm giá tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal maxDiscountAmount;
    
    @NotNull(message = "Ngày hết hạn không được để trống")
    @Future(message = "Ngày hết hạn phải là ngày trong tương lai")
    private LocalDate expiryDate;
    
    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal minOrderValue;
    
    @Min(value = 1, message = "Số lượt sử dụng tối đa phải lớn hơn 0")
    private Integer maxUses;
    
    @lombok.Builder.Default
    private Boolean isActive = true;
}

