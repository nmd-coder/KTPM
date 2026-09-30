package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO chứa raw data từ database để gửi cho AI phân tích
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardInsightsData {
    
    // Revenue data
    private BigDecimal revenueYesterday;
    private BigDecimal revenueLastWeek;
    private BigDecimal revenueGrowth; // % thay đổi
    private List<String> topProducts; // Top 3 sản phẩm bán chạy
    
    // Order data
    private Double cancelRate; // % tỷ lệ hủy đơn
    private List<String> cancelReasons; // Lý do hủy chính
    private Long pendingOrders; // Số đơn chờ xử lý
    private Long totalOrdersLast7Days; // Tổng đơn 7 ngày
    
    // Inventory data
    private List<String> lowStockProducts; // Sản phẩm sắp hết hàng (< 10)
    private List<String> highStockProducts; // Sản phẩm tồn kho cao (> 100)
    
    // Review data
    private Long reviewCount; // Tổng số đánh giá 7 ngày
    private Double positiveReviewRate; // % đánh giá tích cực (≥4 sao)
    private List<String> complaints; // Nội dung phàn nàn chính
}
