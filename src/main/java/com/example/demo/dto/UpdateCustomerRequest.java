package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {
    
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String firstname;
    
    @Size(max = 100, message = "Họ không được vượt quá 100 ký tự")
    private String lastname;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;
    
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;
    
    private String avatar;
    
    private Boolean isActive;
}

