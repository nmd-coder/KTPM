package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerSegmentationService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    
    /**
     * Scheduled task chạy hàng đêm lúc 00:00 (Asia/Ho_Chi_Minh)
     * Phân khúc tất cả khách hàng dựa trên logic:
     * - VIP: total_spent > 5,000,000 VND VÀ orders_count ≥ 3
     * - NEW: Đăng ký trong 30 ngày hoặc đơn đầu tiên trong 30 ngày
     * - AT_RISK: Không mua ≥ 90 ngày, đã từng mua ≥ 1 lần
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void segmentCustomers() {
        log.info("🔄 Bắt đầu phân khúc khách hàng tự động...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<User> customers = userRepository.findAllCustomers();
            LocalDateTime now = LocalDateTime.now();
            
            for (User customer : customers) {
                String segment = determineSegment(customer, now);
                customer.setCustomerSegment(segment);
                customer.setCustomerSegmentUpdatedAt(now);
            }
            
            userRepository.saveAll(customers);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Hoàn thành phân khúc {} khách hàng trong {}ms", customers.size(), duration);
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi phân khúc khách hàng", e);
            throw e;
        }
    }
    
    /**
     * Xác định phân khúc cho một khách hàng
     */
    private String determineSegment(User customer, LocalDateTime now) {
        Long ordersCount = orderRepository.countOrdersByUser(customer.getId());
        BigDecimal totalSpent = orderRepository.getTotalSpentByUser(customer.getId());
        
        // Logic phân khúc theo thứ tự ưu tiên
        // 1. VIP: Chi >5tr VÀ ≥3 đơn
        if (totalSpent != null && totalSpent.compareTo(new BigDecimal("5000000")) > 0 
            && ordersCount >= 3) {
            return "VIP";
        }
        
        // 2. AT_RISK: Không mua ≥90 ngày, đã từng mua
        LocalDateTime lastOrderDate = orderRepository.getLastOrderDateByUser(customer.getId());
        if (lastOrderDate != null && ordersCount > 0) {
            long daysSinceLastOrder = ChronoUnit.DAYS.between(lastOrderDate, now);
            if (daysSinceLastOrder >= 90) {
                return "AT_RISK";
            }
        }
        
        // 3. NEW: Đăng ký ≤30 ngày hoặc đơn đầu ≤30 ngày
        LocalDateTime firstOrderDate = orderRepository.getFirstOrderDateByUser(customer.getId());
        LocalDateTime registrationDate = customer.getCreatedAt();
        
        if (registrationDate != null && ChronoUnit.DAYS.between(registrationDate, now) <= 30) {
            return "NEW";
        }
        
        if (firstOrderDate != null && ChronoUnit.DAYS.between(firstOrderDate, now) <= 30) {
            return "NEW";
        }
        
        // Khách thường - không gán nhãn
        return null;
    }
    
    /**
     * Manual trigger để admin có thể chạy phân khúc ngay
     */
    @Transactional
    public int manualSegment() {
        log.info("🔄 Phân khúc thủ công được kích hoạt");
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<User> customers = userRepository.findAllCustomers();
            LocalDateTime now = LocalDateTime.now();
            
            for (User customer : customers) {
                String segment = determineSegment(customer, now);
                customer.setCustomerSegment(segment);
                customer.setCustomerSegmentUpdatedAt(now);
            }
            
            userRepository.saveAll(customers);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Phân khúc thủ công hoàn thành {} khách hàng trong {}ms", customers.size(), duration);
            
            return customers.size();
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi phân khúc thủ công", e);
            throw e;
        }
    }
}
