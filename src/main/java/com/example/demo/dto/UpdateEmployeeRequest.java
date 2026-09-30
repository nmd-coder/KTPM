package com.example.demo.dto;

import com.example.demo.entity.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {
    
    @Size(max = 100, message = "Họ không được vượt quá 100 ký tự")
    private String firstname;
    
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String lastname;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;
    
    private UserRole role;
    
    @DecimalMin(value = "0.0", message = "Lương giờ phải lớn hơn hoặc bằng 0")
    private BigDecimal salaryPerHour;
    
    private LocalDate hireDate;
    
    private Boolean isActive;
    
    private String avatar;
}

