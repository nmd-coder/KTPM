package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

/**
 * Controller for customer-facing voucher pages
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class VoucherPageController {
    
    /**
     * Display vouchers page for customers
     * Show all available vouchers that customers can use
     */
    @GetMapping("/vouchers")
    public String vouchersPage(Authentication authentication, Model model) {
        log.info("Vouchers page accessed");
        
        // Add authentication info if user is logged in
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("isUserAuthenticated", true);
            model.addAttribute("currentUser", authentication.getName());
        } else {
            model.addAttribute("isUserAuthenticated", false);
        }
        
        model.addAttribute("currentPath", "/vouchers");
        model.addAttribute("pageTitle", "Mã giảm giá - StarShop");
        
        return "customer/vouchers";
    }
}

