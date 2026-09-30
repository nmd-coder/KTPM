package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response DTO containing token and user information
 * Following frontend expectation: { token: "...", user: {...} }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private UserInfoResponse user;
    
    /**
     * Create login response with token and user info
     * @param token JWT token
     * @param user User information
     * @return LoginResponse
     */
    public static LoginResponse of(String token, UserInfoResponse user) {
        return new LoginResponse(token, user);
    }
}
