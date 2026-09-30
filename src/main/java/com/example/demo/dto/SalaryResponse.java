package com.example.demo.dto;

import com.example.demo.entity.Salary;
import com.example.demo.entity.enums.SalaryStatus;
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
public class SalaryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String employeeCode;
    private BigDecimal baseSalary;
    private BigDecimal workingTime;
    private BigDecimal totalSalary;
    private LocalDate monthYear;
    private SalaryStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SalaryResponse fromEntity(Salary salary) {
        return SalaryResponse.builder()
                .id(salary.getId())
                .userId(salary.getUser() != null ? salary.getUser().getId() : null)
                .userName(salary.getUser() != null ? salary.getUser().getFullName() : "Unknown")
                .userEmail(salary.getUser() != null ? salary.getUser().getEmail() : "")
                .employeeCode(salary.getUser() != null ? salary.getUser().getEmployeeCode() : "")
                .baseSalary(salary.getBaseSalary())
                .workingTime(salary.getWorkingTime())
                .totalSalary(salary.getTotalSalary())
                .monthYear(salary.getMonthYear())
                .status(salary.getStatus())
                .notes(salary.getNotes())
                .createdAt(salary.getCreatedAt())
                .updatedAt(salary.getUpdatedAt())
                .build();
    }
}
