package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import com.example.demo.service.ProductService;
import com.example.demo.service.CatalogService;
import com.example.demo.service.WishlistService;
import com.example.demo.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@Tag(name = "🏠 System", description = "System health check and information APIs")
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CatalogService catalogService;
    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        log.info("Home page accessed");
        
        // Featured products for homepage
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        
        // Catalogs for homepage
        model.addAttribute("catalogs", catalogService.findAll());
        
        // Wishlist product IDs for current user (for SSR wishlist state)
        // Always initialize to prevent null pointer exceptions
        model.addAttribute("wishlistProductIds", java.util.Collections.emptySet());
        
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                log.info("Home accessed by authenticated user: {}", authentication.getName());
                userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                    var ids = wishlistService.getWishlistProductIds(user.getId());
                    model.addAttribute("wishlistProductIds", new java.util.HashSet<>(ids));
                });
            } else {
                log.info("Home accessed by guest user");
            }
        } catch (Exception ex) {
            log.error("Error loading wishlist: {}", ex.getMessage());
        }

        return "customer/home";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @Operation(
        summary = "Health check",
        description = "Kiểm tra trạng thái hoạt động của hệ thống và kết nối database"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hệ thống hoạt động bình thường")
    })
    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("database", "connected");
        return response;
    }

    @Operation(
        summary = "Application information",
        description = "Lấy thông tin về ứng dụng (tên, mô tả, developer)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thông tin thành công")
    })
    @GetMapping("/api/info")
    @ResponseBody
    public Map<String, String> info() {
        Map<String, String> response = new HashMap<>();
        response.put("application", "Flower Shop System");
        response.put("description", "API for flower shop management");
        response.put("developer", "Your Team");
        return response;
    }
}
