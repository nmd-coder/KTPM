package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String avatar;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    
    // Statistics
    private Long totalOrders;
    private BigDecimal totalSpent;
    private LocalDateTime lastOrderDate;
    private Long totalReviews;
    
    public String getFullName() {
        return firstname + " " + lastname;
    }
    
    public String getStatusDisplay() {
        return isActive != null && isActive ? "Hoạt động" : "Tạm khóa";
    }
}

