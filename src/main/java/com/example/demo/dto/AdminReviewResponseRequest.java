package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for admin review response request
 */
@Data
public class AdminReviewResponseRequest {
    
    @NotBlank(message = "Phản hồi không được để trống")
    @Size(max = 1000, message = "Phản hồi không được vượt quá 1000 ký tự")
    private String adminResponse;
}
