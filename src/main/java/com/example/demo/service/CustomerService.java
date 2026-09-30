package com.example.demo.service;

import com.example.demo.dto.CreateCustomerRequest;
import com.example.demo.dto.CustomerDTO;
import com.example.demo.dto.UpdateCustomerRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final SessionManagementService sessionManagementService;
    private final com.example.demo.repository.OrderRepository orderRepository;
    
    /**
     * Get all customers (CUSTOMER role only)
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        List<User> customers = userRepository.findByRole(UserRole.CUSTOMER);
        return customers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get customers with pagination
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomers(Pageable pageable) {
        // Get all users and filter for customers only
        List<User> allUsers = userRepository.findAll();
        List<User> customers = allUsers.stream()
            .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .collect(Collectors.toList());
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), customers.size());
        
        List<CustomerDTO> customerDTOs = customers.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(customerDTOs, pageable, customers.size());
    }
    
    /**
     * Get customer by ID
     */
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Người dùng không phải là khách hàng");
        }
        
        return convertToDTO(user);
    }
    
    /**
     * Search customers by keyword
     */
    @Transactional(readOnly = true)
    public List<CustomerDTO> searchCustomers(String keyword) {
        List<User> users = userRepository.searchUsers(keyword);
        return users.stream()
            .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Search customers by keyword with pagination
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchCustomers(String keyword, Pageable pageable) {
        List<User> users = userRepository.searchUsers(keyword);
        List<User> customers = users.stream()
            .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .collect(Collectors.toList());
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), customers.size());
        
        List<CustomerDTO> customerDTOs = customers.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(customerDTOs, pageable, customers.size());
    }
    
    /**
     * Get customers with filters
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getCustomersWithFilters(
            Pageable pageable, 
            String status, 
            String type, 
            String segment,
            String fromDate, 
            String toDate) {
        
        // Get all customers
        List<User> allUsers = userRepository.findAll();
        List<User> customers = allUsers.stream()
            .filter(user -> user.getRole() == UserRole.CUSTOMER)
            .collect(Collectors.toList());
        
        // Apply status filter
        if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                customers = customers.stream()
                    .filter(User::getIsActive)
                    .collect(Collectors.toList());
            } else if ("inactive".equals(status)) {
                customers = customers.stream()
                    .filter(user -> !user.getIsActive())
                    .collect(Collectors.toList());
            }
        }
        
        // Apply segment filter (VIP, NEW, AT_RISK) - uses CustomerSegmentationService logic
        if (segment != null && !segment.isEmpty()) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime thirtyDaysAgo = now.minusDays(30);
            java.time.LocalDateTime ninetyDaysAgo = now.minusDays(90);
            java.math.BigDecimal vipThreshold = new java.math.BigDecimal("5000000"); // 5 million VND
            
            if ("VIP".equals(segment)) {
                // VIP: total_spent > 5M VND AND orders_count >= 3
                customers = customers.stream()
                    .filter(user -> {
                        try {
                            if (user.getOrders() == null) return false;
                            
                            long ordersCount = user.getOrders().stream()
                                .filter(order -> order.getStatus() == com.example.demo.entity.enums.OrderStatus.COMPLETED)
                                .count();
                            
                            java.math.BigDecimal totalSpent = user.getOrders().stream()
                                .filter(order -> order.getStatus() == com.example.demo.entity.enums.OrderStatus.COMPLETED)
                                .map(order -> order.getTotalAmount())
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                            
                            return totalSpent.compareTo(vipThreshold) > 0 && ordersCount >= 3;
                        } catch (Exception e) {
                            log.warn("Error filtering VIP customer {}: {}", user.getId(), e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            } else if ("NEW".equals(segment)) {
                // NEW: Registered within 30 days OR first order within 30 days
                customers = customers.stream()
                    .filter(user -> {
                        try {
                            // Check registration date
                            if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(thirtyDaysAgo)) {
                                return true;
                            }
                            
                            // Check first order date
                            if (user.getOrders() != null && !user.getOrders().isEmpty()) {
                                java.time.LocalDateTime firstOrderDate = user.getOrders().stream()
                                    .map(order -> order.getOrderDate())
                                    .min(java.time.LocalDateTime::compareTo)
                                    .orElse(null);
                                
                                return firstOrderDate != null && firstOrderDate.isAfter(thirtyDaysAgo);
                            }
                            
                            return false;
                        } catch (Exception e) {
                            log.warn("Error filtering NEW customer {}: {}", user.getId(), e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            } else if ("AT_RISK".equals(segment)) {
                // AT_RISK: Has orders but no orders in last 90 days
                customers = customers.stream()
                    .filter(user -> {
                        try {
                            if (user.getOrders() == null || user.getOrders().isEmpty()) {
                                return false; // Never ordered, not at risk
                            }
                            
                            java.time.LocalDateTime lastOrderDate = user.getOrders().stream()
                                .map(order -> order.getOrderDate())
                                .max(java.time.LocalDateTime::compareTo)
                                .orElse(null);
                            
                            return lastOrderDate != null && lastOrderDate.isBefore(ninetyDaysAgo);
                        } catch (Exception e) {
                            log.warn("Error filtering AT_RISK customer {}: {}", user.getId(), e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            }
        }
        
        // Apply type filter (legacy support)
        if (type != null && !type.isEmpty()) {
            if ("loyal".equals(type)) {
                customers = customers.stream()
                    .filter(user -> {
                        try {
                            return user.getOrders() != null && user.getOrders().size() > 10;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            } else if ("new".equals(type)) {
                java.time.LocalDateTime oneMonthAgo = java.time.LocalDateTime.now().minusMonths(1);
                customers = customers.stream()
                    .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(oneMonthAgo))
                    .collect(Collectors.toList());
            } else if ("inactive".equals(type)) {
                java.time.LocalDateTime threeMonthsAgo = java.time.LocalDateTime.now().minusMonths(3);
                customers = customers.stream()
                    .filter(user -> {
                        try {
                            if (user.getOrders() == null || user.getOrders().isEmpty()) {
                                return true;
                            }
                            java.time.LocalDateTime lastOrder = user.getOrders().stream()
                                .map(order -> order.getOrderDate())
                                .max(java.time.LocalDateTime::compareTo)
                                .orElse(null);
                            return lastOrder != null && lastOrder.isBefore(threeMonthsAgo);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            }
        }
        
        // Apply date range filter
        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                java.time.LocalDate from = java.time.LocalDate.parse(fromDate);
                customers = customers.stream()
                    .filter(user -> user.getCreatedAt() != null && 
                           !user.getCreatedAt().toLocalDate().isBefore(from))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Invalid fromDate format: {}", fromDate);
            }
        }
        
        if (toDate != null && !toDate.isEmpty()) {
            try {
                java.time.LocalDate to = java.time.LocalDate.parse(toDate);
                customers = customers.stream()
                    .filter(user -> user.getCreatedAt() != null && 
                           !user.getCreatedAt().toLocalDate().isAfter(to))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Invalid toDate format: {}", toDate);
            }
        }
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), customers.size());
        
        List<CustomerDTO> customerDTOs = customers.subList(start, end).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(customerDTOs, pageable, customers.size());
    }
    
    /**
     * Create new customer
     */
    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        log.info("Creating new customer with email: {}", request.getEmail());
        
        // Validate email and phone uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }
        
        User customer = new User();
        customer.setFirstname(request.getFirstname());
        customer.setLastname(request.getLastname());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setRole(UserRole.CUSTOMER);
        customer.setAvatar(request.getAvatar());
        customer.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        
        User savedCustomer = userRepository.save(customer);
        log.info("Customer created successfully with ID: {}", savedCustomer.getId());
        
        return convertToDTO(savedCustomer);
    }
    
    /**
     * Update customer
     */
    public CustomerDTO updateCustomer(Long id, UpdateCustomerRequest request) {
        log.info("Updating customer with ID: {}", id);
        
        User customer = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Người dùng không phải là khách hàng");
        }
        
        // Update fields if provided
        if (request.getFirstname() != null) {
            customer.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            customer.setLastname(request.getLastname());
        }
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã được sử dụng");
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
            customer.setPhone(request.getPhone());
        }
        if (request.getPassword() != null) {
            customer.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getAvatar() != null) {
            customer.setAvatar(request.getAvatar());
        }
        if (request.getIsActive() != null) {
            customer.setIsActive(request.getIsActive());
        }
        
        User updatedCustomer = userRepository.save(customer);
        log.info("Customer updated successfully with ID: {}", id);
        
        return convertToDTO(updatedCustomer);
    }
    
    /**
     * Check if customer has related data that prevents deletion
     * @return error message if has related data, null if safe to delete
     */
    private String checkRelatedData(User customer) {
        List<String> relatedData = new ArrayList<>();
        
        // Check orders
        long orderCount = orderRepository.countByUserId(customer.getId());
        if (orderCount > 0) {
            relatedData.add(orderCount + " đơn hàng");
        }
        
        if (!relatedData.isEmpty()) {
            return "Không thể xóa vì có dữ liệu liên quan: " + String.join(", ", relatedData) + 
                   ". Vui lòng sử dụng chức năng 'Vô hiệu hóa' thay vì xóa.";
        }
        
        return null;
    }
    
    /**
     * Delete customer - checks for related data first
     */
    public void deleteCustomer(Long id) {
        log.info("Attempting to delete customer with ID: {}", id);
        
        User customer = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Người dùng không phải là khách hàng");
        }
        
        // Check for related data before deletion
        String relatedDataError = checkRelatedData(customer);
        if (relatedDataError != null) {
            log.warn("Cannot delete customer ID {} due to related data", id);
            throw new IllegalStateException(relatedDataError);
        }
        
        // Delete all wishlist items (follows) first - these are safe to delete
        List<com.example.demo.entity.Follow> follows = followRepository.findByUserId(id);
        if (!follows.isEmpty()) {
            log.info("Deleting {} wishlist items for customer ID: {}", follows.size(), id);
            followRepository.deleteAll(follows);
        }
        
        // Safe to delete - no critical related data exists
        userRepository.delete(customer);
        log.info("Customer deleted successfully with ID: {}", id);
    }
    
    /**
     * Toggle customer active status
     */
    public CustomerDTO toggleCustomerStatus(Long id) {
        log.info("Toggling status for customer with ID: {}", id);
        
        User customer = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Người dùng không phải là khách hàng");
        }
        
        boolean wasActive = customer.getIsActive();
        customer.setIsActive(!customer.getIsActive());
        User updatedCustomer = userRepository.save(customer);
        
        // If user was disabled (active -> inactive), invalidate all their sessions
        if (wasActive && !updatedCustomer.getIsActive()) {
            log.info("Customer was disabled, invalidating all sessions for: {}", updatedCustomer.getEmail());
            sessionManagementService.invalidateUserSessions(updatedCustomer.getEmail());
        }
        
        log.info("Customer status toggled successfully with ID: {}", id);
        
        return convertToDTO(updatedCustomer);
    }
    
    /**
     * Get customer statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerStats() {
        List<User> allCustomers = userRepository.findByRole(UserRole.CUSTOMER);
        
        long total = allCustomers.size();
        long loyal = 0;
        java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;
        
        try {
            loyal = allCustomers.stream()
                .filter(c -> {
                    try {
                        return c.getOrders() != null && c.getOrders().size() > 10;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
            
            totalSpent = allCustomers.stream()
                .flatMap(c -> {
                    try {
                        return c.getOrders() != null ? c.getOrders().stream() : java.util.stream.Stream.empty();
                    } catch (Exception e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .map(order -> order.getTotalAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        } catch (Exception e) {
            log.warn("Could not calculate customer statistics: {}", e.getMessage());
        }
        
        // Count new customers this month
        java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.now()
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
        
        long newThisMonth = allCustomers.stream()
            .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(startOfMonth))
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("loyal", loyal);
        stats.put("totalSpent", totalSpent);
        stats.put("newThisMonth", newThisMonth);
        
        return stats;
    }
    
    /**
     * Get AI customer segment statistics
     * VIP: total_spent > 5M VND AND orders_count >= 3 (matches CustomerSegmentationService)
     * NEW: Customers registered within last 30 days OR first order within 30 days
     * AT_RISK: Customers with no orders in last 90 days (but have at least 1 order)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSegmentStats() {
        List<User> allCustomers = userRepository.findByRole(UserRole.CUSTOMER);
        
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        java.time.LocalDateTime ninetyDaysAgo = java.time.LocalDateTime.now().minusDays(90);
        java.math.BigDecimal vipThreshold = new java.math.BigDecimal("5000000"); // 5 million VND (matches CustomerSegmentationService)
        
        long vipCount = 0;
        long newCount = 0;
        long atRiskCount = 0;
        
        try {
            // VIP: total_spent > 5M VND AND orders_count >= 3 (matches CustomerSegmentationService)
            vipCount = allCustomers.stream()
                .filter(c -> {
                    try {
                        if (c.getOrders() == null) return false;
                        
                        long ordersCount = c.getOrders().stream()
                            .filter(order -> order.getStatus() == com.example.demo.entity.enums.OrderStatus.COMPLETED)
                            .count();
                        
                        java.math.BigDecimal totalSpent = c.getOrders().stream()
                            .filter(order -> order.getStatus() == com.example.demo.entity.enums.OrderStatus.COMPLETED)
                            .map(order -> order.getTotalAmount())
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                        
                        return totalSpent.compareTo(vipThreshold) > 0 && ordersCount >= 3;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
            
            // NEW: Registered within 30 days OR first order within 30 days
            newCount = allCustomers.stream()
                .filter(c -> {
                    try {
                        // Check registration date
                        if (c.getCreatedAt() != null && c.getCreatedAt().isAfter(thirtyDaysAgo)) {
                            return true;
                        }
                        
                        // Check first order date
                        if (c.getOrders() != null && !c.getOrders().isEmpty()) {
                            java.time.LocalDateTime firstOrderDate = c.getOrders().stream()
                                .map(order -> order.getOrderDate())
                                .min(java.time.LocalDateTime::compareTo)
                                .orElse(null);
                            
                            return firstOrderDate != null && firstOrderDate.isAfter(thirtyDaysAgo);
                        }
                        
                        return false;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
            
            // AT_RISK: Customers with no recent orders
            atRiskCount = allCustomers.stream()
                .filter(c -> {
                    try {
                        if (c.getOrders() == null || c.getOrders().isEmpty()) {
                            return false; // Never ordered, not at risk
                        }
                        
                        java.time.LocalDateTime lastOrderDate = c.getOrders().stream()
                            .map(order -> order.getOrderDate())
                            .max(java.time.LocalDateTime::compareTo)
                            .orElse(null);
                        
                        return lastOrderDate != null && lastOrderDate.isBefore(ninetyDaysAgo);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
                
        } catch (Exception e) {
            log.warn("Could not calculate segment statistics: {}", e.getMessage());
        }
        
        Map<String, Object> segmentStats = new HashMap<>();
        segmentStats.put("vip", vipCount);
        segmentStats.put("new", newCount);
        segmentStats.put("atRisk", atRiskCount);
        
        return segmentStats;
    }
    
    /**
     * Convert User entity to CustomerDTO
     */
    private CustomerDTO convertToDTO(User user) {
        // Calculate total spent - safe null handling
        java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;
        java.time.LocalDateTime lastOrderDate = null;
        long totalOrders = 0;
        long totalReviews = 0;
        
        try {
            if (user.getOrders() != null) {
                totalOrders = user.getOrders().size();
                totalSpent = user.getOrders().stream()
                    .map(order -> order.getTotalAmount())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                
                lastOrderDate = user.getOrders().stream()
                    .map(order -> order.getOrderDate())
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Could not load order data for customer {}: {}", user.getId(), e.getMessage());
        }
        
        try {
            if (user.getReviews() != null) {
                totalReviews = user.getReviews().size();
            }
        } catch (Exception e) {
            log.warn("Could not load review data for customer {}: {}", user.getId(), e.getMessage());
        }
        
        return CustomerDTO.builder()
            .id(user.getId())
            .firstname(user.getFirstname())
            .lastname(user.getLastname())
            .email(user.getEmail())
            .phone(user.getPhone())
            .avatar(user.getAvatar())
            .isActive(user.getIsActive())
            .lastLogin(user.getLastLogin())
            .createdAt(user.getCreatedAt())
            .totalOrders(totalOrders)
            .totalSpent(totalSpent)
            .lastOrderDate(lastOrderDate)
            .totalReviews(totalReviews)
            .build();
    }
}

