package com.example.demo.controller;

import com.example.demo.dto.CartDTO;
import com.example.demo.dto.CartRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Controller for managing user shopping cart functionality
 * Provides both web pages and REST API endpoints
 */
@Controller
@Tag(name = "🛒 Cart", description = "Shopping cart management APIs - Add, update, remove items")
public class CartController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Display user's cart page
     */
    @GetMapping("/cart")
    public String cartPage(Authentication authentication, Model model) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            // Get user's cart
            CartResponse cartResponse = cartService.getCart(user.getId());
            
            // Add breadcrumbs
            addBreadcrumb(model, "Trang chủ", "/");
            addBreadcrumb(model, "Giỏ hàng", "/cart");
            
            // Add model attributes
            if (cartResponse.isSuccess()) {
                model.addAttribute("cart", cartResponse.getCart());
                model.addAttribute("totalItems", cartResponse.getTotalItems());
            } else {
                // Create empty cart for error case
                CartDTO emptyCart = new CartDTO();
                emptyCart.setUserId(user.getId());
                emptyCart.setTotalAmount(java.math.BigDecimal.ZERO);
                emptyCart.setTotalQuantity(0);
                emptyCart.setItems(new java.util.ArrayList<>());
                model.addAttribute("cart", emptyCart);
                model.addAttribute("totalItems", 0L);
                model.addAttribute("error", cartResponse.getMessage());
            }
            
            return "cart/index";
            
        } catch (Exception e) {
            logger.error("Error displaying cart page: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi tải giỏ hàng");
            return "error/500";
        }
    }
    
    /**
     * REST API: Add product to cart
     */
    @Operation(
        summary = "Thêm sản phẩm vào giỏ hàng",
        description = "Thêm một sản phẩm vào giỏ hàng của người dùng. Nếu sản phẩm đã tồn tại, số lượng sẽ được cộng dồn.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thêm thành công"),
        @ApiResponse(responseCode = "400", description = "Request không hợp lệ hoặc sản phẩm không khả dụng"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<CartResponse>> addToCart(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin sản phẩm cần thêm",
                required = true,
                content = @Content(schema = @Schema(implementation = CartRequest.class))
            )
            @Valid @RequestBody CartRequest request,
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
            
            CartResponse response = cartService.addToCart(user.getId(), request.getProductId(), request.getQuantity());
            
            if (response.isSuccess()) {
                logger.info("User {} added product {} to cart", user.getEmail(), request.getProductId());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error adding to cart: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi thêm vào giỏ hàng"));
        }
    }
    
    /**
     * REST API: Update cart item quantity
     */
    @Operation(
        summary = "Cập nhật số lượng sản phẩm",
        description = "Cập nhật số lượng của một sản phẩm trong giỏ hàng. Đặt quantity = 0 để xóa sản phẩm.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "400", description = "Request không hợp lệ"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PutMapping("/api/cart/update")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<CartResponse>> updateCartItem(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin cập nhật số lượng",
                required = true
            )
            @Valid @RequestBody CartRequest request,
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
            
            CartResponse response = cartService.updateCartItem(user.getId(), request.getProductId(), request.getQuantity());
            
            if (response.isSuccess()) {
                logger.info("User {} updated cart item {}", user.getEmail(), request.getProductId());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error updating cart item: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi cập nhật giỏ hàng"));
        }
    }
    
    /**
     * REST API: Remove product from cart
     */
    @Operation(
        summary = "Xóa sản phẩm khỏi giỏ hàng",
        description = "Xóa hoàn toàn một sản phẩm khỏi giỏ hàng",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
        @ApiResponse(responseCode = "400", description = "Sản phẩm không tồn tại trong giỏ"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @DeleteMapping("/api/cart/remove")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<CartResponse>> removeFromCart(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "ID sản phẩm cần xóa",
                required = true
            )
            @Valid @RequestBody CartRequest request,
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
            
            CartResponse response = cartService.removeFromCart(user.getId(), request.getProductId());
            
            if (response.isSuccess()) {
                logger.info("User {} removed product {} from cart", user.getEmail(), request.getProductId());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error removing from cart: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi xóa khỏi giỏ hàng"));
        }
    }
    
    /**
     * REST API: Get user's cart
     */
    @Operation(
        summary = "Lấy thông tin giỏ hàng",
        description = "Lấy toàn bộ thông tin giỏ hàng của người dùng hiện tại",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @GetMapping("/api/cart/get")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<CartResponse>> getCart(
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
            
            CartResponse response = cartService.getCart(user.getId());
            
            return ResponseEntity.ok(ResponseWrapper.success(response));
            
        } catch (Exception e) {
            logger.error("Error getting cart: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy giỏ hàng"));
        }
    }
    
    /**
     * REST API: Clear user's cart
     */
    @Operation(
        summary = "Xóa toàn bộ giỏ hàng",
        description = "Xóa tất cả sản phẩm trong giỏ hàng của người dùng",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @DeleteMapping("/api/cart/clear")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<CartResponse>> clearCart(
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
            
            CartResponse response = cartService.clearCart(user.getId());
            
            if (response.isSuccess()) {
                logger.info("User {} cleared cart", user.getEmail());
                return ResponseEntity.ok(ResponseWrapper.success(response));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(response.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error clearing cart: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi xóa giỏ hàng"));
        }
    }
    
    /**
     * REST API: Get cart items count
     */
    @Operation(
        summary = "Đếm số sản phẩm trong giỏ",
        description = "Lấy tổng số lượng sản phẩm trong giỏ hàng (không yêu cầu authentication, trả về 0 nếu chưa đăng nhập)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công")
    })
    @GetMapping("/api/cart/count")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Long>> getCartCount(
            @Parameter(hidden = true) Authentication authentication) {
        
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(ResponseWrapper.success(0L));
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.ok(ResponseWrapper.success(0L));
            }
            
            Long count = cartService.getCartItemsCount(user.getId());
            
            return ResponseEntity.ok(ResponseWrapper.success(count));
            
        } catch (Exception e) {
            logger.error("Error getting cart count: {}", e.getMessage());
            return ResponseEntity.ok(ResponseWrapper.success(0L));
        }
    }
}
