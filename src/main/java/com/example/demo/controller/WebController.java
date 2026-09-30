package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web Controller for serving Thymeleaf templates
 * Following rules.mdc specifications for MVC pattern
 */
@Controller
@Slf4j
public class WebController {

    // Login and register pages moved to AuthPageController to avoid mapping conflicts

    /**
     * Serve forgot password page
     * @param error Error message if any
     * @param success Success message if any
     * @param model Spring Model
     * @return forgot-password template name
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(value = "error", required = false) String error,
                                   @RequestParam(value = "success", required = false) String success,
                                   Model model) {
        
        log.info("Serving forgot password page with error: {}, success: {}", error, success);
        
        if (error != null) {
            model.addAttribute("error", error);
        }
        
        if (success != null) {
            model.addAttribute("success", success);
        }
        
        return "auth/forgot-password";
    }

    /**
     * Serve reset password page
     * @param token Reset token
     * @param error Error message if any
     * @param model Spring Model
     * @return reset-password template name
     */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "token", required = false) String token,
                                  @RequestParam(value = "error", required = false) String error,
                                  Model model) {
        
        log.info("Serving reset password page with token: {}, error: {}", 
                token != null ? token.substring(0, 10) + "..." : null, error);
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("Reset password page accessed without token");
            return "redirect:/forgot-password?error=Token không hợp lệ";
        }
        
        model.addAttribute("token", token);
        
        if (error != null) {
            model.addAttribute("error", error);
        }
        
        return "auth/reset-password";
    }
}
