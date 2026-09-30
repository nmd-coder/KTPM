package com.example.demo.dto;

import com.example.demo.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long id;
    private String employeeCode;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String avatar;
    private UserRole role;
    private String position;
    private String department;
    private BigDecimal salaryPerHour;
    private LocalDate hireDate;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    
    // Statistics
    private Long totalShifts;
    private BigDecimal totalHoursWorked;
    private Long monthsWorked;
    
    public String getFullName() {
        return firstname + " " + lastname;
    }
    
    public String getStatusDisplay() {
        return isActive != null && isActive ? "Hoạt động" : "Tạm khóa";
    }
    
    public String getRoleDisplay() {
        if (role == null) return "";
        return role == UserRole.ADMIN ? "Quản trị viên" : "Nhân viên";
    }
}

