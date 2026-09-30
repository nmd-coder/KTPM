package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.entity.Address;
import com.example.demo.dto.UserProfileDTO;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Account Controller for usedor account management pages
 * Following rules.mdc specifications for MVC pattern
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final FollowRepository followRepository;
    private final AddressRepository addressRepository;


    /**
     * Serve account information page
     * Only accessible to authenticated users
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public String accountProfile(Authentication authentication, Model model) {
        log.info("Account profile page accessed by user: {}", authentication.getName());
        
        // Use optimized query to get user profile with statistics in one query
        UserProfileDTO userProfile = userRepository.findUserProfileByEmail(authentication.getName()).orElse(null);
        if (userProfile != null) {
            User user = userProfile.getUser();
            
            // User basic info
            model.addAttribute("userObject", user);
            model.addAttribute("userFullName", user.getFullName());
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("userPhone", user.getPhone());
            model.addAttribute("userRole", user.getRole().getDisplayName());
            model.addAttribute("memberSince", user.getCreatedAt() != null ? 
                user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy")) : "2024");
            
            // Get default address - already fetched with JOIN FETCH
            if (!user.getAddresses().isEmpty()) {
                // Try to find default address first
                Address defaultAddress = user.getAddresses().stream()
                    .filter(addr -> addr.getIsDefault() != null && addr.getIsDefault())
                    .findFirst()
                    .orElse(user.getAddresses().get(0)); // Fallback to first address
                
                model.addAttribute("userAddress", defaultAddress.getFullAddress());
            } else {
                model.addAttribute("userAddress", "");
            }
            
            // Use pre-calculated statistics from DTO
            model.addAttribute("ordersCount", userProfile.getOrdersCount());
            model.addAttribute("wishlistCount", userProfile.getWishlistCount());
            model.addAttribute("cartCount", userProfile.getCartItemsCount());
        }
        
        // Authentication info
        model.addAttribute("isUserAuthenticated", authentication.isAuthenticated());
        model.addAttribute("currentUser", authentication.getName());
        model.addAttribute("pageTitle", "Thông tin tài khoản");
        return "account/profile";
    }


    /**
     * Serve order history page
     * Only accessible to authenticated users
     */
    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public String accountOrders(Authentication authentication, Model model) {
        log.info("Account orders page accessed by user: {}", authentication.getName());
        
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("userObject", user);
            
            // Get user's orders and statistics
            Long ordersCount = orderRepository.countOrdersByUser(user.getId());
            Long wishlistCount = followRepository.countByUserId(user.getId());
            
            model.addAttribute("ordersCount", ordersCount);
            model.addAttribute("wishlistCount", wishlistCount);
        } else {
            model.addAttribute("ordersCount", 0L);
            model.addAttribute("wishlistCount", 0L);
        }
        
        model.addAttribute("isUserAuthenticated", authentication.isAuthenticated());
        model.addAttribute("currentUser", authentication.getName());
        model.addAttribute("pageTitle", "Lịch sử đơn hàng");
        return "account/orders";
    }

    /**
     * Update user profile information
     * Only accessible to authenticated users
     */
    @PostMapping("/profile/update")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public String updateProfile(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("phone") String phone,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        log.info("Profile update requested by user: {}", authentication.getName());
        
        try {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng");
                return "redirect:/account/profile";
            }
            
            // Normalize and validate phone (digits only, 10 chars)
            String normalizedPhone = phone == null ? null : phone.trim();
            if (normalizedPhone != null && !normalizedPhone.matches("\\d{10}")) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ. Vui lòng nhập 10 chữ số.");
                return "redirect:/account/profile";
            }

            // Validate phone uniqueness if changed
            if (normalizedPhone != null && !normalizedPhone.equals(user.getPhone())) {
                boolean phoneInUse = userRepository.existsByPhone(normalizedPhone);
                if (phoneInUse) {
                    redirectAttributes.addFlashAttribute("error", "Số điện thoại đã được sử dụng bởi tài khoản khác");
                    return "redirect:/account/profile";
                }
            }

            // Update user information
            user.setFirstname(firstName.trim());
            user.setLastname(lastName.trim());
            String oldPhone = user.getPhone();
            if (normalizedPhone != null) {
                user.setPhone(normalizedPhone);
            }
            
            // Save user
            userRepository.saveAndFlush(user);
            
            log.info("Profile updated successfully for user: {}", authentication.getName());
            log.info("Phone changed from {} to {} for userId={}", oldPhone, user.getPhone(), user.getId());
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
            
        } catch (Exception e) {
            log.error("Error updating profile for user {}: {}", authentication.getName(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật thông tin. Vui lòng thử lại.");
        }
        
        return "redirect:/account/profile";
    }

    // Wishlist functionality moved to WishlistController (/wishlist)
}
