package com.example.demo.service;

import com.example.demo.dto.AiInsightResponse;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service for admin statistics and analytics
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AdminAiInsightsService adminAiInsightsService;

    /**
     * Get dashboard statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Basic counts
            stats.put("totalUsers", userRepository.count());
            stats.put("totalProducts", productRepository.count());
            stats.put("totalOrders", orderRepository.count());
            
            // Revenue statistics
            BigDecimal totalRevenue = orderRepository.getTotalRevenue();
            stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            
            // Monthly revenue
            BigDecimal monthlyRevenue = orderRepository.getMonthlyRevenue();
            stats.put("monthlyRevenue", monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);
            
            // Order status counts
            stats.put("pendingOrders", orderRepository.countByStatus(OrderStatus.PENDING));
            stats.put("processingOrders", orderRepository.countByStatus(OrderStatus.PROCESSING));
            stats.put("shippedOrders", orderRepository.countByStatus(OrderStatus.SHIPPED));
            stats.put("completedOrders", orderRepository.countByStatus(OrderStatus.COMPLETED));
            stats.put("cancelledOrders", orderRepository.countByStatus(OrderStatus.CANCELLED));
            
            // Recent orders
            stats.put("recentOrders", orderRepository.findTop10ByOrderByOrderDateDesc());
            
            
        } catch (Exception e) {
            log.error("Error generating dashboard stats: {}", e.getMessage(), e);
            // Return empty stats on error
            stats.put("totalUsers", 0L);
            stats.put("totalProducts", 0L);
            stats.put("totalOrders", 0L);
            stats.put("totalRevenue", BigDecimal.ZERO);
            stats.put("monthlyRevenue", BigDecimal.ZERO);
            stats.put("pendingOrders", 0L);
            stats.put("processingOrders", 0L);
            stats.put("shippedOrders", 0L);
            stats.put("completedOrders", 0L);
            stats.put("cancelledOrders", 0L);
        }
        
        return stats;
    }

    /**
     * Get revenue chart data for the last 12 months
     */
    public Map<String, Object> getRevenueChartData() {
        Map<String, Object> chartData = new HashMap<>();
        
        try {
            List<Object[]> monthlyData = orderRepository.getMonthlyRevenueChart();
            
            String[] months = new String[12];
            BigDecimal[] revenues = new BigDecimal[12];
            
            // Initialize with zeros
            for (int i = 0; i < 12; i++) {
                LocalDateTime date = LocalDateTime.now().minusMonths(11 - i);
                months[i] = date.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                revenues[i] = BigDecimal.ZERO;
            }
            
            // Fill with actual data
            for (Object[] data : monthlyData) {
                String month = (String) data[0];
                BigDecimal revenue = (BigDecimal) data[1];
                
                // Find matching month and update
                for (int i = 0; i < 12; i++) {
                    if (months[i].equals(month)) {
                        revenues[i] = revenue;
                        break;
                    }
                }
            }
            
            chartData.put("months", months);
            chartData.put("revenues", revenues);
            
        } catch (Exception e) {
            log.error("Error generating revenue chart data: {}", e.getMessage(), e);
            chartData.put("months", new String[0]);
            chartData.put("revenues", new BigDecimal[0]);
        }
        
        return chartData;
    }

    /**
     * Get order status chart data
     */
    public Map<String, Object> getOrderStatusChartData() {
        Map<String, Object> chartData = new HashMap<>();
        
        try {
            chartData.put("pending", orderRepository.countByStatus(OrderStatus.PENDING));
            chartData.put("processing", orderRepository.countByStatus(OrderStatus.PROCESSING));
            chartData.put("shipped", orderRepository.countByStatus(OrderStatus.SHIPPED));
            chartData.put("completed", orderRepository.countByStatus(OrderStatus.COMPLETED));
            chartData.put("cancelled", orderRepository.countByStatus(OrderStatus.CANCELLED));
            
        } catch (Exception e) {
            log.error("Error generating order status chart data: {}", e.getMessage(), e);
            chartData.put("pending", 0L);
            chartData.put("processing", 0L);
            chartData.put("shipped", 0L);
            chartData.put("completed", 0L);
            chartData.put("cancelled", 0L);
        }
        
        return chartData;
    }

    /**
     * Get correlation data between customers and orders for the last 7 days
     */
    public Map<String, Object> getCorrelationChartData(int days) {
        Map<String, Object> chartData = new HashMap<>();
        
        try {
            List<Object[]> dailyStats = orderRepository.getDailyStatsForDays(days);
            
            String[] labels = new String[days];
            Long[] orders = new Long[days];
            Long[] customers = new Long[days];
            BigDecimal[] revenues = new BigDecimal[days];
            
            // Initialize with zeros and dates
            for (int i = 0; i < days; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(days - 1 - i);
                labels[i] = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                orders[i] = 0L;
                customers[i] = 0L;
                revenues[i] = BigDecimal.ZERO;
            }
            
            // Fill with actual data
            for (Object[] data : dailyStats) {
                String dateStr = data[0].toString();
                Long orderCount = ((Number) data[1]).longValue();
                Long customerCount = ((Number) data[2]).longValue();
                BigDecimal revenue = (BigDecimal) data[3];
                
                // Find matching date and update
                for (int i = 0; i < days; i++) {
                    if (labels[i].equals(dateStr)) {
                        orders[i] = orderCount;
                        customers[i] = customerCount;
                        revenues[i] = revenue != null ? revenue : BigDecimal.ZERO;
                        break;
                    }
                }
            }
            
            chartData.put("labels", labels);
            chartData.put("orders", orders);
            chartData.put("customers", customers);
            chartData.put("revenue", revenues);
            
        } catch (Exception e) {
            log.error("Error generating correlation chart data: {}", e.getMessage(), e);
            // Return empty data on error
            String[] emptyLabels = new String[days];
            Long[] emptyOrders = new Long[days];
            Long[] emptyCustomers = new Long[days];
            BigDecimal[] emptyRevenues = new BigDecimal[days];
            
            for (int i = 0; i < days; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(days - 1 - i);
                emptyLabels[i] = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                emptyOrders[i] = 0L;
                emptyCustomers[i] = 0L;
                emptyRevenues[i] = BigDecimal.ZERO;
            }
            
            chartData.put("labels", emptyLabels);
            chartData.put("orders", emptyOrders);
            chartData.put("customers", emptyCustomers);
            chartData.put("revenue", emptyRevenues);
        }
        
        return chartData;
    }

    /**
     * Get revenue trend data for the last N days
     */
    public Map<String, Object> getRevenueTrendData(int days) {
        Map<String, Object> trendData = new HashMap<>();
        
        try {
            List<Object[]> dailyRevenue = orderRepository.getDailyRevenueForDays(days);
            
            String[] labels = new String[days];
            BigDecimal[] revenues = new BigDecimal[days];
            
            // Initialize with zeros and dates
            for (int i = 0; i < days; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(days - 1 - i);
                labels[i] = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                revenues[i] = BigDecimal.ZERO;
            }
            
            // Fill with actual data
            for (Object[] data : dailyRevenue) {
                String dateStr = data[0].toString();
                BigDecimal revenue = (BigDecimal) data[1];
                
                // Find matching date and update
                for (int i = 0; i < days; i++) {
                    if (labels[i].equals(dateStr)) {
                        revenues[i] = revenue != null ? revenue : BigDecimal.ZERO;
                        break;
                    }
                }
            }
            
            trendData.put("labels", labels);
            trendData.put("revenue", revenues);
            
            // Calculate current value (latest revenue)
            BigDecimal currentRevenue = revenues[days - 1];
            trendData.put("currentValue", currentRevenue);
            
        } catch (Exception e) {
            log.error("Error generating revenue trend data: {}", e.getMessage(), e);
            // Return empty data on error
            String[] emptyLabels = new String[days];
            BigDecimal[] emptyRevenues = new BigDecimal[days];
            
            for (int i = 0; i < days; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(days - 1 - i);
                emptyLabels[i] = date.format(DateTimeFormatter.ofPattern("dd/MM"));
                emptyRevenues[i] = BigDecimal.ZERO;
            }
            
            trendData.put("labels", emptyLabels);
            trendData.put("revenue", emptyRevenues);
            trendData.put("currentValue", BigDecimal.ZERO);
        }
        
        return trendData;
    }

    /**
     * Get AI insights for dashboard
     */
    public AiInsightResponse getAiInsights() {
        try {
            return adminAiInsightsService.getAiInsights();
        } catch (Exception e) {
            log.error("Error getting AI insights", e);
            // Return fallback insights
            AiInsightResponse.InsightItem fallbackItem = AiInsightResponse.InsightItem.builder()
                    .type("info")
                    .icon("🤖")
                    .title("Tạm thời không có dữ liệu")
                    .message("Không thể tải phân tích AI. Vui lòng thử lại sau.")
                    .severity("info")
                    .build();
            
            return AiInsightResponse.builder()
                    .insights(List.of(fallbackItem))
                    .build();
        }
    }
}
