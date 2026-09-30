package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSuggestionRequest {
    
    /**
     * Mục tiêu của voucher: NEW_CUSTOMER, INCREASE_AOV, CLEAR_INVENTORY
     */
    @NotBlank(message = "Mục tiêu không được để trống")
    private String objective;
    
    /**
     * Tên sản phẩm cụ thể cho mục tiêu đẩy hàng tồn kho (tùy chọn)
     */
    private String targetProduct;
}
