package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@Slf4j
public class AuthPageController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "success", required = false) String success,
                          Model model) {
        
        log.info("Serving login page with error: {}, success: {}", error, success);
        
        if (error != null) {
            switch (error) {
                case "oauth2":
                    model.addAttribute("error", "OAuth2 authentication failed. Please try again.");
                    break;
                case "invalid":
                    model.addAttribute("error", "Invalid email or password.");
                    break;
                case "expired":
                    model.addAttribute("error", "Your session has expired. Please login again.");
                    break;
                default:
                    model.addAttribute("error", "Login failed. Please try again.");
            }
        }
        
        if (success != null) {
            switch (success) {
                case "logout":
                    model.addAttribute("success", "You have been logged out successfully.");
                    break;
                case "register":
                    model.addAttribute("success", "Registration successful! Please login with your credentials.");
                    break;
                default:
                    model.addAttribute("success", success);
            }
        }
        
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(value = "error", required = false) String error,
                              @RequestParam(value = "success", required = false) String success,
                              Model model) {
        
        log.info("Serving register page with error: {}, success: {}", error, success);
        
        if (error != null) {
            model.addAttribute("error", error);
        }
        
        if (success != null) {
            model.addAttribute("success", success);
        }
        
        return "auth/register";
    }
    
    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("Logout request received");
        
        // Invalidate session
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        
        // Clear authentication cookies
        response.addCookie(createExpiredCookie("authToken"));
        response.addCookie(createExpiredCookie("JSESSIONID"));
        response.addCookie(createExpiredCookie("SPRING_SECURITY_REMEMBER_ME_COOKIE"));
        
        // Redirect to login page with success message
        response.sendRedirect("/login?success=logout");
    }
    
    private jakarta.servlet.http.Cookie createExpiredCookie(String name) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        return cookie;
    }

}
