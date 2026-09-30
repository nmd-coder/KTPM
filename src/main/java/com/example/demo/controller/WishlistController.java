package com.example.demo.controller;

import com.example.demo.dto.WishlistDTO;
import com.example.demo.dto.WishlistRequest;
import com.example.demo.dto.WishlistResponse;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller for managing user wishlist functionality
 * Provides both web pages and REST API endpoints
 */
@Controller
@Validated
@Tag(name = "❤️ Wishlist", description = "Wishlist/Favorites management APIs - Save favorite products")
public class WishlistController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);
    
    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Display user's wishlist page
     */
    @GetMapping({"/wishlist", "/account/wishlist"})
    public String wishlistPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "followedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication,
            Model model) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/auth/login?returnUrl=/wishlist";
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            // Setup pagination and sorting
            Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            // Get user's wishlist
            Page<WishlistDTO> wishlist = wishlistService.getUserWishlist(user.getId(), pageable);
            Long totalWishlist = wishlistService.getUserWishlistCount(user.getId());
            
            // Add breadcrumbs
            addBreadcrumb(model, "Trang chủ", "/");
            addBreadcrumb(model, "Danh sách yêu thích", "/wishlist");
            
            // Add user object for template
            model.addAttribute("userObject", user);
            model.addAttribute("currentUser", user.getEmail());
            
            // Add wishlist data
            model.addAttribute("wishlist", wishlist);
            model.addAttribute("totalWishlist", totalWishlist);
            model.addAttribute("wishlistCount", totalWishlist); // For compatibility
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", wishlist.getTotalPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sort);
            model.addAttribute("sortDirection", direction);
            model.addAttribute("hasWishlist", !wishlist.isEmpty());
            
            // Add member since year for sidebar
            if (user.getCreatedAt() != null) {
                model.addAttribute("memberSince", String.valueOf(user.getCreatedAt().getYear()));
            } else {
                model.addAttribute("memberSince", "2024");
            }
            
            // Add counts for sidebar badges
            model.addAttribute("ordersCount", 0L); // Will be populated by OrderService if needed
            
            // SEO attributes
            model.addAttribute("pageTitle", "Danh sách yêu thích - StarShop");
            model.addAttribute("pageDescription", "Danh sách các sản phẩm bạn đã yêu thích tại StarShop");
            
            logger.info("Displaying wishlist page for user: {} with {} items", user.getEmail(), wishlist.getTotalElements());
            
            return "account/wishlist";
            
        } catch (Exception e) {
            logger.error("Error displaying wishlist page: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi tải danh sách yêu thích");
            return "error/500";
        }
    }
    
    /**
     * REST API: Toggle wishlist/favorite status
     */
    @Operation(
        summary = "Toggle trạng thái yêu thích",
        description = "Bật/tắt trạng thái yêu thích cho sản phẩm. Nếu đã tồn tại thì xóa, chưa có thì thêm.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Toggle thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/wishlist/toggle")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<WishlistResponse>> toggleWishlist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "ID sản phẩm cần toggle",
                required = true
            )
            @Valid @RequestBody WishlistRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            WishlistResponse response = wishlistService.toggleWishlist(user.getId(), request.getProductId());
            
            if (response.isSuccess()) {
                logger.info("User {} toggled wishlist for product {}", user.getEmail(), request.getProductId());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error toggling wishlist: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi thay đổi trạng thái wishlist"));
        }
    }
    
    /**
     * REST API: Add product to wishlist
     */
    @Operation(
        summary = "Thêm sản phẩm vào danh sách yêu thích",
        description = "Thêm một sản phẩm vào danh sách yêu thích của người dùng",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thêm thành công"),
        @ApiResponse(responseCode = "400", description = "Sản phẩm đã có trong wishlist"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/wishlist/add")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<WishlistResponse>> addToWishlist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "ID sản phẩm cần thêm",
                required = true
            )
            @Valid @RequestBody WishlistRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            WishlistResponse response = wishlistService.addToWishlist(user.getId(), request.getProductId());
            
            if (response.isSuccess()) {
                logger.info("User {} added product {} to wishlist", user.getEmail(), request.getProductId());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error adding to wishlist: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi thêm vào wishlist"));
        }
    }
    
    /**
     * REST API: Remove product from wishlist
     */
    @Operation(
        summary = "Xóa sản phẩm khỏi danh sách yêu thích",
        description = "Xóa một sản phẩm khỏi danh sách yêu thích của người dùng",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
        @ApiResponse(responseCode = "400", description = "Sản phẩm không có trong wishlist"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @DeleteMapping("/api/wishlist/remove")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<WishlistResponse>> removeFromWishlist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "ID sản phẩm cần xóa",
                required = true
            )
            @Valid @RequestBody WishlistRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            WishlistResponse response = wishlistService.removeFromWishlist(user.getId(), request.getProductId());
            
            if (response.isSuccess()) {
                logger.info("User {} removed product {} from wishlist", user.getEmail(), request.getProductId());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error removing from wishlist: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi xóa khỏi wishlist"));
        }
    }
    
    /**
     * REST API: Get wishlist status for a product
     */
    @Operation(
        summary = "Kiểm tra trạng thái yêu thích",
        description = "Kiểm tra xem sản phẩm có trong danh sách yêu thích hay không",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @GetMapping("/api/wishlist/status/{productId}")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<WishlistResponse>> getWishlistStatus(
            @Parameter(description = "ID sản phẩm cần kiểm tra", required = true)
            @PathVariable Long productId,
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            WishlistResponse response = wishlistService.getWishlistStatus(user.getId(), productId);
            
            return ResponseEntity.ok(ResponseWrapper.success(response));
            
        } catch (Exception e) {
            logger.error("Error getting wishlist status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy trạng thái wishlist"));
        }
    }
    
    /**
     * REST API: Get user's wishlist list
     */
    @Operation(
        summary = "Lấy danh sách yêu thích",
        description = "Lấy toàn bộ danh sách sản phẩm yêu thích của người dùng",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @GetMapping("/api/wishlist/list")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<List<WishlistDTO>>> getWishlistList(
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            List<WishlistDTO> wishlist = wishlistService.getUserWishlist(user.getId());
            
            return ResponseEntity.ok(ResponseWrapper.success(wishlist));
            
        } catch (Exception e) {
            logger.error("Error getting wishlist list: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy danh sách yêu thích"));
        }
    }
    
    /**
     * REST API: Clear all wishlist items
     */
    @Operation(
        summary = "Xóa toàn bộ danh sách yêu thích",
        description = "Xóa tất cả sản phẩm khỏi danh sách yêu thích",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @DeleteMapping("/api/wishlist/clear")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<WishlistResponse>> clearWishlist(
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            WishlistResponse response = wishlistService.clearUserWishlist(user.getId());
            
            if (response.isSuccess()) {
                logger.info("User {} cleared all wishlist items", user.getEmail());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error clearing wishlist: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi xóa danh sách yêu thích"));
        }
    }
}
