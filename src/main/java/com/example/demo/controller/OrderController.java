package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.entity.Address;
import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.entity.enums.PaymentMethod;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OrderService;
import com.example.demo.service.PaymentService;
import com.example.demo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing user orders functionality
 * Provides both web pages and REST API endpoints
 */
@Controller
@Tag(name = "📦 Orders", description = "Order management APIs - Create, view, cancel orders")
public class OrderController extends BaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private ReviewService reviewService;
    
    /**
     * Display user's orders page
     */
    @GetMapping("/orders")
    public String ordersPage(Authentication authentication, Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String payment,
                            @RequestParam(required = false) String message) {
        try {
            logger.info("Orders page accessed - Authentication: {}", authentication != null ? authentication.getName() : "null");
            logger.info("Authentication isAuthenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
            
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("User not authenticated, redirecting to login");
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            // Handle payment result messages
            if ("failed".equals(payment)) {
                model.addAttribute("paymentError", true);
                model.addAttribute("errorMessage", message != null ? message : "Thanh toán thất bại. Vui lòng thử lại.");
            }
            
            // Get user's orders
            Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
            Page<OrderDTO> orders = orderService.getUserOrders(user.getId(), pageable);
            
            // Add breadcrumbs
            addBreadcrumb(model, "Trang chủ", "/");
            addBreadcrumb(model, "Đơn hàng của tôi", "/orders");
            
            // Add model attributes
            model.addAttribute("orders", orders);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", orders.getTotalPages());
            model.addAttribute("totalElements", orders.getTotalElements());
            
            return "account/orders";
            
        } catch (Exception e) {
            logger.error("Error displaying orders page: {}", e.getMessage());
            return "error/500";
        }
    }
    
    /**
     * Display order detail page
     */
    @GetMapping("/orders/{orderId}")
    public String orderDetailPage(@PathVariable String orderId, 
                            @RequestParam(required = false) String payment,
                            @RequestParam(required = false) String transId,
                            Authentication authentication, Model model) {
        try {
            logger.info("Order detail page accessed - orderId: {}, Authentication: {}", 
                orderId, authentication != null ? authentication.getName() : "null");
            logger.info("Authentication isAuthenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
            
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("User not authenticated for order detail, redirecting to login");
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user == null) {
                logger.warn("User not found for email: {}", authentication.getName());
                return "redirect:/auth/login";
            }
            
            logger.info("Getting order details for orderId: {}, userId: {}", orderId, user.getId());
            
            // Get order details
            OrderResponse orderResponse = orderService.getOrder(orderId, user.getId());
            
            if (!orderResponse.isSuccess()) {
                logger.warn("Order not found or access denied: orderId={}, userId={}, message={}", 
                    orderId, user.getId(), orderResponse.getMessage());
                return "redirect:/orders?error=" + orderResponse.getMessage();
            }
            
            // Handle payment result messages
            if ("success".equals(payment)) {
                model.addAttribute("paymentSuccess", true);
                model.addAttribute("successMessage", "Thanh toán thành công! Đơn hàng của bạn đang được xử lý.");
                if (transId != null) {
                    model.addAttribute("transactionId", transId);
                }
            } else if ("failed".equals(payment)) {
                model.addAttribute("paymentError", true);
                model.addAttribute("errorMessage", "Thanh toán thất bại. Vui lòng thử lại hoặc chọn phương thức thanh toán khác.");
            }
            
            // Add breadcrumbs
            addBreadcrumb(model, "Trang chủ", "/");
            addBreadcrumb(model, "Đơn hàng của tôi", "/orders");
            addBreadcrumb(model, "Chi tiết đơn hàng #" + orderId, "/orders/" + orderId);
            
            // Add model attributes - pass OrderResponse instead of OrderDTO
            model.addAttribute("order", orderResponse);
            
            return "orders/detail";
            
        } catch (Exception e) {
            logger.error("Error displaying order detail page for orderId {}: {}", orderId, e.getMessage(), e);
            return "redirect:/orders?error=Có lỗi xảy ra khi tải chi tiết đơn hàng";
        }
    }
    
    /**
     * Display checkout page
     */
    @GetMapping("/checkout")
    @Transactional(readOnly = true)
    public String checkoutPage(Authentication authentication, Model model) {
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/auth/login";
            }
            
            User user = userRepository.findByEmailWithAddresses(authentication.getName()).orElse(null);
            if (user == null) {
                return "redirect:/auth/login";
            }
            
            // Validate user profile before checkout
            String validationError = validateUserProfileForCheckout(user);
            if (validationError != null) {
                model.addAttribute("error", validationError);
                model.addAttribute("requireProfileUpdate", true);
                model.addAttribute("user", user);
                
                // Add breadcrumbs
                addBreadcrumb(model, "Trang chủ", "/");
                addBreadcrumb(model, "Giỏ hàng", "/cart");
                addBreadcrumb(model, "Thanh toán", "/checkout");
                
                // Add page metadata
                model.addAttribute("pageTitle", "Cập nhật thông tin - StarShop");
                model.addAttribute("pageDescription", "Vui lòng cập nhật thông tin tài khoản để tiếp tục");
                
                return "orders/checkout";
            }
            
            // Add breadcrumbs
            addBreadcrumb(model, "Trang chủ", "/");
            addBreadcrumb(model, "Giỏ hàng", "/cart");
            addBreadcrumb(model, "Thanh toán", "/checkout");
            
            // Add user info for checkout form
            model.addAttribute("user", user);
            
            // Add user's addresses
            model.addAttribute("addresses", user.getAddresses());
            
            // Find default address
            Address defaultAddress = user.getAddresses().stream()
                .filter(Address::getIsDefault)
                .findFirst()
                .orElse(null);
            model.addAttribute("defaultAddress", defaultAddress);
            
            // Add payment methods
            Map<String, Object> paymentMethods = paymentService.getAvailablePaymentMethods();
            model.addAttribute("paymentMethods", paymentMethods);
            
            // Add page metadata
            model.addAttribute("pageTitle", "Thanh toán - StarShop");
            model.addAttribute("pageDescription", "Hoàn tất đơn hàng của bạn tại StarShop");
            
            return "orders/checkout";
            
        } catch (Exception e) {
            logger.error("Error displaying checkout page: {}", e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang thanh toán");
            return "error/500";
        }
    }
    
    /**
     * REST API: Create order from cart
     */
    @Operation(
        summary = "Tạo đơn hàng từ giỏ hàng",
        description = "Tạo đơn hàng mới từ các sản phẩm trong giỏ hàng. Giỏ hàng sẽ được xóa sau khi tạo đơn thành công.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tạo đơn thành công"),
        @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc giỏ hàng trống"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/orders/create-from-cart")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<OrderResponse>> createOrderFromCart(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin đơn hàng (địa chỉ, payment method, voucher, notes)",
                required = true
            )
            @Valid @RequestBody OrderRequest request,
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
            
            // Create order from cart
            OrderResponse orderResponse = orderService.createOrderFromCart(user.getId(), request);
            
            if (orderResponse.isSuccess()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(orderResponse.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error creating order from cart: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi tạo đơn hàng"));
        }
    }
    
    /**
     * REST API: Create order directly
     */
    @Operation(
        summary = "Tạo đơn hàng trực tiếp",
        description = "Tạo đơn hàng mới không qua giỏ hàng. Phải cung cấp danh sách sản phẩm (items) trong request.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tạo đơn thành công"),
        @ApiResponse(responseCode = "400", description = "Thông tin không hợp lệ hoặc sản phẩm hết hàng"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/orders/create-direct")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<OrderResponse>> createOrderDirect(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Thông tin đơn hàng với danh sách items, address, payment method",
                required = true
            )
            @Valid @RequestBody OrderRequest request,
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
            
            // Create order directly
            OrderResponse orderResponse = orderService.createOrderDirect(user.getId(), request);
            
            if (orderResponse.isSuccess()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(orderResponse.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error creating direct order: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi tạo đơn hàng"));
        }
    }
    
    /**
     * REST API: Get user's orders
     */
    @Operation(
        summary = "Lấy danh sách đơn hàng",
        description = "Lấy danh sách tất cả đơn hàng của người dùng hiện tại với phân trang. Sắp xếp theo ngày tạo mới nhất.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @GetMapping("/api/orders/list")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Page<OrderDTO>>> getUserOrders(
            @Parameter(description = "Trang số (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by status", example = "PENDING")
            @RequestParam(required = false) String status,
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
            
            // Get user's orders with optional status filter
            Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
            Page<OrderDTO> orders;
            
            if (status != null && !status.isEmpty()) {
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    orders = orderService.getUserOrdersByStatus(user.getId(), orderStatus, pageable);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                        .body(ResponseWrapper.error("Trạng thái đơn hàng không hợp lệ"));
                }
            } else {
                orders = orderService.getUserOrders(user.getId(), pageable);
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(orders));
            
        } catch (Exception e) {
            logger.error("Error getting user orders: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy danh sách đơn hàng"));
        }
    }
    
    /**
     * REST API: Get order details
     */
    @Operation(
        summary = "Lấy chi tiết đơn hàng",
        description = "Lấy thông tin chi tiết của một đơn hàng theo ID. Chỉ lấy được đơn hàng của chính mình.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "400", description = "Không tìm thấy đơn hàng hoặc không có quyền truy cập"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @GetMapping("/api/orders/{orderId}")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<OrderResponse>> getOrder(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable String orderId,
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
            
            // Get order details
            OrderResponse orderResponse = orderService.getOrder(orderId, user.getId());
            
            if (orderResponse.isSuccess()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(orderResponse.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error getting order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy thông tin đơn hàng"));
        }
    }
    
    /**
     * REST API: Cancel order
     */
    @Operation(
        summary = "Hủy đơn hàng",
        description = "Hủy đơn hàng theo ID. Chỉ được hủy đơn ở trạng thái PENDING hoặc PROCESSING.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hủy thành công"),
        @ApiResponse(responseCode = "400", description = "Không thể hủy đơn hàng (sai trạng thái hoặc không có quyền)"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PutMapping("/api/orders/{orderId}/cancel")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<OrderResponse>> cancelOrder(
            @Parameter(description = "ID đơn hàng cần hủy", required = true)
            @PathVariable String orderId,
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
            
            // Cancel order
            OrderResponse orderResponse = orderService.cancelOrder(orderId, user.getId());
            
            if (orderResponse.isSuccess()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(orderResponse.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi hủy đơn hàng"));
        }
    }
    
    /**
     * REST API: Confirm order received by user
     */
    @Operation(
        summary = "Xác nhận đã nhận hàng",
        description = "User xác nhận đã nhận được hàng. Sau khi xác nhận, user có thể đánh giá đơn hàng.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xác nhận thành công"),
        @ApiResponse(responseCode = "400", description = "Không thể xác nhận (sai trạng thái hoặc không có quyền)"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PutMapping("/api/orders/{orderId}/confirm-received")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<OrderResponse>> confirmOrderReceived(
            @Parameter(description = "ID đơn hàng cần xác nhận", required = true)
            @PathVariable String orderId,
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
            
            // Confirm order received
            OrderResponse orderResponse = orderService.confirmOrderReceived(orderId, user.getId());
            
            if (orderResponse.isSuccess()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderResponse));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(orderResponse.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error confirming order received {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi xác nhận đơn hàng"));
        }
    }
    
    /**
     * REST API: Update order status (Admin/Staff only)
     */
    @Operation(
        summary = "Cập nhật trạng thái đơn hàng (Admin/Staff)",
        description = "Cập nhật trạng thái đơn hàng. Chỉ Admin và Staff có quyền sử dụng. Trạng thái: PENDING, PROCESSING, SHIPPED, COMPLETED, RECEIVED, CANCELLED.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
        @ApiResponse(responseCode = "400", description = "Trạng thái không hợp lệ"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
        @ApiResponse(responseCode = "403", description = "Không có quyền (yêu cầu Admin/Staff)")
    })
    @PutMapping("/api/orders/{orderId}/status")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<OrderDTO>> updateOrderStatus(
            @Parameter(description = "ID đơn hàng", required = true)
            @PathVariable String orderId,
            @Parameter(description = "Trạng thái mới", required = true, example = "PROCESSING")
            @RequestParam OrderStatus status,
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
            
            // Check if user has admin/staff role
            if (!user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
                return ResponseEntity.status(403)
                    .body(ResponseWrapper.error("Bạn không có quyền thực hiện thao tác này"));
            }
            
            // Update order status
            OrderDTO updatedOrder = orderService.updateOrderStatus(orderId, status);
            
            return ResponseEntity.ok(ResponseWrapper.success(updatedOrder));
            
        } catch (Exception e) {
            logger.error("Error updating order {} status: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi cập nhật trạng thái đơn hàng"));
        }
    }
    
    /**
     * REST API: Reorder from previous order
     */
    @Operation(
        summary = "Mua lại đơn hàng",
        description = "Tạo đơn hàng mới dựa trên đơn hàng cũ. Sản phẩm sẽ được thêm vào giỏ hàng và chuyển đến trang checkout.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mua lại thành công"),
        @ApiResponse(responseCode = "400", description = "Không thể mua lại đơn hàng này"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/orders/{orderId}/reorder")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Map<String, Object>>> reorder(
            @Parameter(description = "ID đơn hàng cũ", required = true)
            @PathVariable String orderId,
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
            
            // Process reorder
            Map<String, Object> result = orderService.reorderFromPreviousOrder(orderId, user.getId());
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(ResponseWrapper.success(result, "Mua lại thành công! Sản phẩm đã được thêm vào giỏ hàng."));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error((String) result.get("message")));
            }
            
        } catch (Exception e) {
            logger.error("Error reordering from order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi mua lại đơn hàng"));
        }
    }
    
    /**
     * REST API: Get orders by status (Admin/Staff only)
     */
    @Operation(
        summary = "Lấy đơn hàng theo trạng thái (Admin/Staff)",
        description = "Lấy tất cả đơn hàng theo trạng thái cụ thể. Chỉ Admin và Staff có quyền. Dùng để quản lý và theo dõi đơn hàng.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thành công"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực"),
        @ApiResponse(responseCode = "403", description = "Không có quyền (yêu cầu Admin/Staff)")
    })
    @GetMapping("/api/orders/status/{status}")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<List<OrderDTO>>> getOrdersByStatus(
            @Parameter(description = "Trạng thái đơn hàng", required = true, example = "PENDING")
            @PathVariable OrderStatus status,
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
            
            // Check if user has admin/staff role
            if (!user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
                return ResponseEntity.status(403)
                    .body(ResponseWrapper.error("Bạn không có quyền thực hiện thao tác này"));
            }
            
            // Get orders by status with pagination
            Pageable pageable = PageRequest.of(0, 100); // Get first 100 orders
            Page<OrderDTO> orderPage = orderService.getOrdersByStatus(status, pageable);
            List<OrderDTO> orders = orderPage.getContent();
            
            return ResponseEntity.ok(ResponseWrapper.success(orders));
            
        } catch (Exception e) {
            logger.error("Error getting orders by status {}: {}", status, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy danh sách đơn hàng"));
        }
    }
    
    /**
     * REST API: Process payment for an order
     */
    @Operation(
        summary = "Xử lý thanh toán cho đơn hàng",
        description = "Khởi tạo quá trình thanh toán cho đơn hàng. Hỗ trợ COD và MoMo. Nếu là MoMo sẽ trả về payment URL.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thanh toán thành công hoặc lấy payment URL"),
        @ApiResponse(responseCode = "400", description = "Đơn hàng không hợp lệ hoặc lỗi thanh toán"),
        @ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    @PostMapping("/api/orders/{orderId}/payment")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<PaymentService.PaymentResult>> processPayment(
            @PathVariable String orderId,
            @RequestParam PaymentMethod paymentMethod,
            Authentication authentication) {
        
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
            
            // Get order entity
            Order order = orderService.getOrderEntity(orderId, user.getId());
            if (order == null) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Đơn hàng không tồn tại hoặc bạn không có quyền truy cập"));
            }
            
            // Process payment
            PaymentService.PaymentResult paymentResult = paymentService.processPayment(order, paymentMethod);
            
            if (paymentResult.isSuccess()) {
                logger.info("Payment processed successfully for order {} by user {}", orderId, user.getEmail());
                return ResponseEntity.ok(ResponseWrapper.success(paymentResult));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(paymentResult.getMessage()));
            }
            
        } catch (Exception e) {
            logger.error("Error processing payment for order {}: {}", orderId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi xử lý thanh toán"));
        }
    }
    
    /**
     * REST API: Get available payment methods
     */
    @GetMapping("/api/payment/methods")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Map<String, Object>>> getPaymentMethods() {
        
        try {
            Map<String, Object> paymentMethods = paymentService.getAvailablePaymentMethods();
            return ResponseEntity.ok(ResponseWrapper.success(paymentMethods));
            
        } catch (Exception e) {
            logger.error("Error getting payment methods: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi lấy danh sách phương thức thanh toán"));
        }
    }
    
    /**
     * REST API: Create order with payment processing
     */
    @PostMapping("/api/orders/create-with-payment")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Map<String, Object>>> createOrderWithPayment(
            @Valid @RequestBody OrderWithPaymentRequest request,
            Authentication authentication) {
        User user = null;
        try {
            // Check authentication
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Vui lòng đăng nhập để sử dụng tính năng này"));
            }
            
            user = userRepository.findByEmailWithAddresses(authentication.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401)
                    .body(ResponseWrapper.error("Người dùng không hợp lệ"));
            }
            
            // Validate user profile before creating order
            String validationError = validateUserProfileForCheckout(user);
            if (validationError != null) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(validationError));
            }
            
            // Validate payment method
            if (request.getPaymentMethod() == null) {
                logger.error("PaymentMethod is null in request!");
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Phương thức thanh toán không hợp lệ"));
            }
            
            OrderRequest orderRequest = request.getOrderRequest();
            logger.info("Creating order for user {} with addressId: {}, paymentMethod: {}", 
                user.getId(), 
                orderRequest.getAddressId(), 
                request.getPaymentMethod());
            
            // Create order with payment (transactional)
            Map<String, Object> result = orderService.createOrderWithPayment(
                user.getId(), 
                orderRequest, 
                request.getPaymentMethod(),
                paymentService
            );
            
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                logger.info("Order created and payment processed successfully for user {}", user.getEmail());
                return ResponseEntity.ok(ResponseWrapper.success(result));
            } else {
                PaymentService.PaymentResult paymentResult = (PaymentService.PaymentResult) result.get("payment");
                String errorMessage = paymentResult != null ? paymentResult.getMessage() : "Payment failed";
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error(errorMessage));
            }
            
        } catch (Exception e) {
            logger.error("Error creating order with payment for user {}: {}",
                (user != null ? user.getEmail() : "unknown"), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(ResponseWrapper.error("Có lỗi xảy ra khi tạo đơn hàng: " + e.getMessage()));
        }
    }
    
    // Request DTO for order with payment
    public static class OrderWithPaymentRequest {
        private OrderRequest orderRequest;
        private PaymentMethod paymentMethod;
        
        public OrderRequest getOrderRequest() {
            return orderRequest;
        }
        
        public void setOrderRequest(OrderRequest orderRequest) {
            this.orderRequest = orderRequest;
        }
        
        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }
        
        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }
    
    /**
     * Validate user profile for checkout
     * @param user User to validate
     * @return Error message if validation fails, null if valid
     */
    private String validateUserProfileForCheckout(User user) {
        // Check phone number
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            return "Vui lòng cập nhật số điện thoại trong tài khoản của bạn để tiếp tục thanh toán.";
        }
        
        // Validate Vietnamese phone number (10 digits, starts with 0)
        String phone = user.getPhone().trim().replaceAll("\\s+", "");
        if (!phone.matches("^0[0-9]{9}$")) {
            return "Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam gồm 10 chữ số (bắt đầu bằng số 0).";
        }
        
        // Check if user has at least one address
        if (user.getAddresses() == null || user.getAddresses().isEmpty()) {
            return "Vui lòng thêm địa chỉ giao hàng trong tài khoản của bạn để tiếp tục thanh toán.";
        }
        
        // Check if user has a default address or at least one valid address
        boolean hasValidAddress = user.getAddresses().stream()
            .anyMatch(addr -> addr.getStreet() != null && !addr.getStreet().trim().isEmpty());
        
        if (!hasValidAddress) {
            return "Vui lòng cập nhật địa chỉ giao hàng trong tài khoản của bạn.";
        }
        
        return null; // All validations passed
    }
    
    /**
     * Get reviews for a specific order
     */
    @GetMapping("/api/orders/{orderId}/reviews")
    @ResponseBody
    @Operation(
        summary = "Lấy đánh giá của đơn hàng",
        description = "Lấy tất cả đánh giá của các sản phẩm trong đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy đánh giá thành công"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập đơn hàng này")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ResponseWrapper<List<com.example.demo.dto.ReviewResponse>>> getOrderReviews(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            logger.info("User {} getting reviews for order: {}", authentication.getName(), orderId);
            
            // Get user
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get order and verify ownership
            OrderDTO orderDTO = orderService.getOrderById(orderId);
            if (orderDTO == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if user owns this order
            if (!orderDTO.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403)
                    .body(ResponseWrapper.error("Bạn không có quyền xem đánh giá của đơn hàng này"));
            }
            
            // Get reviews for this order - use ReviewService instead
            List<com.example.demo.dto.ReviewResponse> reviews = reviewService.getReviewsByOrderId(orderId);
            
            return ResponseEntity.ok(ResponseWrapper.success(reviews));
            
        } catch (Exception e) {
            logger.error("Error getting reviews for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy đánh giá đơn hàng: " + e.getMessage()));
        }
    }
}
