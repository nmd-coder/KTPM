package com.example.demo.dto;

import com.example.demo.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherSuggestionResponse {
    
    /**
     * Mã voucher được AI gợi ý
     */
    private String code;
    
    /**
     * Tên voucher
     */
    private String name;
    
    /**
     * Mô tả voucher
     */
    private String description;
    
    /**
     * Loại giảm giá (PERCENTAGE hoặc FIXED)
     */
    private DiscountType discountType;
    
    /**
     * Giá trị giảm giá
     */
    private BigDecimal discountValue;
    
    /**
     * Giảm giá tối đa (cho PERCENTAGE)
     */
    private BigDecimal maxDiscountAmount;
    
    /**
     * Đơn hàng tối thiểu
     */
    private BigDecimal minOrderValue;
    
    /**
     * Ngày hết hạn
     */
    private String expiryDate;
    
    /**
     * Số lượt sử dụng tối đa
     */
    private Integer maxUses;
    
    /**
     * Lời giải thích của AI về lý do đề xuất cấu hình này
     */
    private String explanation;
    
    /**
     * Danh sách cảnh báo nếu có xung đột với voucher hiện tại
     */
    private List<String> warnings;
    
    /**
     * Metadata về dữ liệu đã sử dụng để tạo gợi ý
     */
    private String dataUsed;
}
