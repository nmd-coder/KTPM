package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Base controller to add common model attributes to all views
 * Now includes PaymentController for proper authentication handling
 */
@ControllerAdvice(assignableTypes = {
    WishlistController.class,
    OrderController.class,
    ProductController.class,
    CategoryController.class,
    HomeController.class,
    AccountController.class,
    AdminController.class,
    AdminProductController.class,
    BlogController.class,
    VoucherPageController.class,
    CartController.class,
    StaffController.class,
    PaymentController.class
})
public class BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private FollowRepository followRepository;

    /**
     * Add current path to all views for navigation highlighting
     */
    @ModelAttribute("currentPath")
    public String addCurrentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
    
    /**
     * Add username to all views
     */
    @ModelAttribute("currentUser")
    public String addUsername(Authentication authentication) {
        if (authentication != null && 
            authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            return authentication.getName();
        }
        return null;
    }
    
    /**
     * Add authentication status to all views
     */
    @ModelAttribute("isUserAuthenticated")
    public boolean addAuthenticationStatus(Authentication authentication) {
        return authentication != null && 
               authentication.isAuthenticated() && 
               !authentication.getName().equals("anonymousUser");
    }
    
    /**
     * Add user object to all views
     */
    @ModelAttribute("userObject")
    @Transactional(readOnly = true)
    public User addUserObject(Authentication authentication) {
        try {
            if (authentication != null && 
                authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                return userRepository.findByEmail(authentication.getName()).orElse(null);
            }
        } catch (Exception e) {
            // Silently handle user object error
        }
        return null;
    }

    @ModelAttribute("userRole")
    @Transactional(readOnly = true)
    public String addUserRole(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                return user.getRole().name();
            }
        }
        return null;
    }
    
    /**
     * Add cart count to all views
     */
    @ModelAttribute("cartCount")
    @Transactional(readOnly = true)
    public Long addCartCount(Authentication authentication) {
        try {
            if (authentication != null && 
                authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                User user = userRepository.findByEmail(authentication.getName()).orElse(null);
                if (user != null) {
                    return cartService.getCartItemsCount(user.getId());
                }
            }
        } catch (Exception e) {
            // Silently handle cart count error
        }
        return 0L;
    }
    /**
     * Add wishlist count to all views
     */
    @ModelAttribute("wishlistCount")
    @Transactional(readOnly = true)
    public Long addWishlistCount(Authentication authentication) {
        try {
            if (authentication != null && 
                authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                User user = userRepository.findByEmail(authentication.getName()).orElse(null);
                if (user != null) {
                    return followRepository.countByUserId(user.getId());
                }
            }
        } catch (Exception e) {
            // Silently handle wishlist count error
        }
        return 0L;
    }
    
    /**
     * Add orders count to all views
     */
    @Transactional(readOnly = true)
    public int addOrdersCount(Authentication authentication) {
        try {
            if (authentication != null && 
                authentication.isAuthenticated() && 
                !authentication.getName().equals("anonymousUser")) {
                User user = userRepository.findByEmail(authentication.getName()).orElse(null);
                if (user != null && user.getOrders() != null) {
                    return user.getOrders().size();
                }
            }
        } catch (Exception e) {
            // Silently handle orders count error
        }
        return 0;
    }
    
    /**
     * Breadcrumb item class
     */
    public static class BreadcrumbItem {
        private String title;
        private String url;
        
        public BreadcrumbItem(String title, String url) {
            this.title = title;
            this.url = url;
        }
        
        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }
    
    /**
     * Add breadcrumb item to model
     */
    protected void addBreadcrumb(Model model, String title, String url) {
        @SuppressWarnings("unchecked")
        List<BreadcrumbItem> breadcrumbs = (List<BreadcrumbItem>) model.asMap().get("breadcrumbs");
        if (breadcrumbs == null) {
            breadcrumbs = new ArrayList<>();
            model.addAttribute("breadcrumbs", breadcrumbs);
        }
        breadcrumbs.add(new BreadcrumbItem(title, url));
    }
    
    /**
     * Get user ID from session (for chat and staff controllers)
     */
    protected Long getUserIdFromSession(jakarta.servlet.http.HttpSession session) {
        return (Long) session.getAttribute("userId");
    }
    
    /**
     * Get user ID from authentication
     */
    protected Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && 
            authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }
}
