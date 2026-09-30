package com.example.demo.repository;

import com.example.demo.entity.Order;
import com.example.demo.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    List<Order> findByStatus(OrderStatus status);
    
    @RestResource(path = "findByStatusPaged", rel = "findByStatusPaged")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.id = :orderId")
    Order findOrderWithItems(@Param("orderId") String orderId);
    
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.user " +
           "LEFT JOIN FETCH o.address " +
           "LEFT JOIN FETCH o.deliveryUnit " +
           "LEFT JOIN FETCH o.voucher " +
           "WHERE o.id = :orderId")
    Order findOrderWithAllDetails(@Param("orderId") String orderId);
    
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE o.id = :orderId")
    Order findOrderWithItemsAndProducts(@Param("orderId") String orderId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countOrdersByUser(@Param("userId") Long userId);
    
    // Count orders by user ID (for deletion check)
    long countByUserId(Long userId);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.id = :userId AND o.status = 'COMPLETED'")
    BigDecimal getTotalSpentByUser(@Param("userId") Long userId);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.orderDate ASC")
    List<Order> findPendingOrders(@Param("status") OrderStatus status);
    
    // Dashboard statistics queries
    Long countByStatus(OrderStatus status);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' " +
           "AND YEAR(o.orderDate) = YEAR(CURRENT_DATE) AND MONTH(o.orderDate) = MONTH(CURRENT_DATE)")
    BigDecimal getMonthlyRevenue();
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user ORDER BY o.orderDate DESC LIMIT 10")
    List<Order> findTop10ByOrderByOrderDateDesc();
    
    @Query(value = "SELECT TO_CHAR(order_date, 'MM/YYYY') as month, SUM(total_amount) as revenue " +
           "FROM orders WHERE status = 'COMPLETED' " +
           "AND order_date >= CURRENT_DATE - INTERVAL '12 months' " +
           "GROUP BY TO_CHAR(order_date, 'MM/YYYY'), DATE_TRUNC('month', order_date) " +
           "ORDER BY DATE_TRUNC('month', order_date)", nativeQuery = true)
    List<Object[]> getMonthlyRevenueChart();
    
    // Daily statistics for correlation chart
    @Query(value = "SELECT " +
           "TO_CHAR(DATE_TRUNC('day', order_date), 'DD/MM') as day, " +
           "COUNT(DISTINCT id) as order_count, " +
           "COUNT(DISTINCT user_id) as customer_count, " +
           "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN total_amount ELSE 0 END), 0) as revenue " +
           "FROM orders " +
           "WHERE order_date >= CURRENT_DATE - INTERVAL '1 day' * ?1 " +
           "GROUP BY DATE_TRUNC('day', order_date) " +
           "ORDER BY DATE_TRUNC('day', order_date)", nativeQuery = true)
    List<Object[]> getDailyStatsForDays(int days);
    
    // Daily revenue for trend chart
    @Query(value = "SELECT " +
           "TO_CHAR(DATE_TRUNC('day', order_date), 'DD/MM') as day, " +
           "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN total_amount ELSE 0 END), 0) as revenue " +
           "FROM orders " +
           "WHERE order_date >= CURRENT_DATE - INTERVAL '1 day' * ?1 " +
           "GROUP BY DATE_TRUNC('day', order_date) " +
           "ORDER BY DATE_TRUNC('day', order_date)", nativeQuery = true)
    List<Object[]> getDailyRevenueForDays(int days);
    
    // Search orders by order number, customer name, or email
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user u WHERE " +
           "CAST(o.id AS string) LIKE CONCAT('%', :searchTerm, '%') OR " +
           "LOWER(CONCAT(u.firstname, ' ', u.lastname)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Order> searchOrders(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // AI Insights queries
    // Lấy doanh thu 24h gần nhất (thay vì chỉ hôm qua để tránh = 0)
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE status = 'COMPLETED' AND order_date >= NOW() - INTERVAL '24 hours'", 
           nativeQuery = true)
    BigDecimal getYesterdayRevenue();
    
    // Lấy doanh thu 24h trước đó (từ 48h đến 24h trước) để so sánh growth
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE status = 'COMPLETED' " +
           "AND order_date >= NOW() - INTERVAL '48 hours' " +
           "AND order_date < NOW() - INTERVAL '24 hours'", 
           nativeQuery = true)
    BigDecimal getPreviousDayRevenue();
    
    // Lấy doanh thu 7 ngày gần nhất (thay vì chỉ tuần trước để có dữ liệu thực tế)
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
           "WHERE status = 'COMPLETED' AND order_date >= NOW() - INTERVAL '7 days'", 
           nativeQuery = true)
    BigDecimal getLastWeekRevenue();
    
    @Query(value = "SELECT COUNT(*) FROM orders " +
           "WHERE status = 'CANCELLED' AND order_date >= CURRENT_DATE - INTERVAL '7 days'", 
           nativeQuery = true)
    Long getCancelledOrdersLast7Days();
    
    @Query(value = "SELECT COUNT(*) FROM orders " +
           "WHERE order_date >= CURRENT_DATE - INTERVAL '7 days'", 
           nativeQuery = true)
    Long getTotalOrdersLast7Days();
    
    /**
     * Get average order value (AOV) for completed orders in the last 30 days
     */
    @Query("SELECT AVG(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' " +
           "AND o.orderDate >= :startDate")
    BigDecimal getAverageOrderValue(@Param("startDate") LocalDateTime startDate);
    
    // AI Customer Segmentation queries
    @Query("SELECT MAX(o.orderDate) FROM Order o WHERE o.user.id = :userId")
    LocalDateTime getLastOrderDateByUser(@Param("userId") Long userId);
    
    @Query("SELECT MIN(o.orderDate) FROM Order o WHERE o.user.id = :userId")
    LocalDateTime getFirstOrderDateByUser(@Param("userId") Long userId);
}
