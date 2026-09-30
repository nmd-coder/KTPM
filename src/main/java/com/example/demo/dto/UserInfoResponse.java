package com.example.demo.dto;

import com.example.demo.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User information response DTO for token validation
 * Following rules.mdc specifications for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    
    private Long id;
    private String email;
    private String firstname;
    private String lastname;
    private String phone;
    private String avatar;
    private UserRole role;
    private Boolean isActive;
}
