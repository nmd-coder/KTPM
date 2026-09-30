package com.example.demo.controller;

import com.example.demo.dto.AdminReviewResponseRequest;
import com.example.demo.dto.OrderReviewGroupDTO;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.dto.ReviewAiAnalysisResponse;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ReviewService;
import com.example.demo.service.AdminAiInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Admin REST API Controller for Order-Grouped Review Management
 * Following IMPLEMENTATION_PLAN.md specifications
 */
@RestController
@RequestMapping("/admin/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "👑 Admin Order Reviews", description = "Admin APIs - Manage reviews grouped by order")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final AdminAiInsightsService adminAiInsightsService;

    /**
     * Get reviews grouped by order with pagination
     */
    @GetMapping("/by-orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Lấy đánh giá nhóm theo đơn hàng",
        description = "Lấy danh sách đánh giá được nhóm theo đơn hàng với phân trang (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<ResponseWrapper<Page<OrderReviewGroupDTO>>> getReviewsGroupedByOrder(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số đánh giá mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Tìm kiếm theo tên khách hàng hoặc mã đơn hàng", example = "Nguyen Van A")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sắp xếp", example = "newest")
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication) {
        
        try {
            log.info("Admin {} getting reviews grouped by order: page={}, size={}, search={}, sort={}", 
                    authentication.getName(), page, size, search, sort);
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Get reviews grouped by order
            Page<OrderReviewGroupDTO> orderReviewsPage = reviewService.getReviewsGroupedByOrder(pageable, search, sort);
            
            return ResponseEntity.ok(ResponseWrapper.success(orderReviewsPage));
            
        } catch (Exception e) {
            log.error("Error getting reviews grouped by order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy danh sách đánh giá theo đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Get order review group by order ID
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Lấy chi tiết đánh giá theo đơn hàng",
        description = "Lấy chi tiết tất cả đánh giá trong một đơn hàng (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại hoặc chưa có đánh giá")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> getOrderReviewGroup(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            log.info("Admin {} getting order review group for order: {}", authentication.getName(), orderId);
            
            Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error getting order review group for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy chi tiết đánh giá đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Add bulk admin response to all reviews in an order
     */
    @PostMapping("/orders/{orderId}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Phản hồi tất cả đánh giá trong đơn hàng",
        description = "Admin phản hồi cho tất cả đánh giá trong một đơn hàng cùng lúc"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phản hồi thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> addBulkAdminResponse(
            @PathVariable String orderId,
            @Valid @RequestBody AdminReviewResponseRequest request,
            Authentication authentication) {
        
        try {
            log.info("Admin {} adding bulk response to order {}", authentication.getName(), orderId);
            
            User adminUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            // Add bulk admin response
            List<Review> updatedReviews = reviewService.addBulkAdminResponse(orderId, adminUser.getId(), request.getAdminResponse());
            
            // Get updated order review group
            Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                log.info("Bulk admin response added successfully to {} reviews in order {}", updatedReviews.size(), orderId);
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get(), 
                    String.format("Phản hồi đã được thêm cho %d đánh giá trong đơn hàng", updatedReviews.size())));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Không thể lấy thông tin đơn hàng sau khi cập nhật"));
            }
            
        } catch (Exception e) {
            log.error("Error adding bulk admin response to order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể thêm phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Update bulk admin response for all reviews in an order
     */
    @PutMapping("/orders/{orderId}/response")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cập nhật phản hồi tất cả đánh giá trong đơn hàng",
        description = "Admin cập nhật phản hồi cho tất cả đánh giá trong một đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật phản hồi thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> updateBulkAdminResponse(
            @PathVariable String orderId,
            @Valid @RequestBody AdminReviewResponseRequest request,
            Authentication authentication) {
        
        try {
            log.info("Admin {} updating bulk response for order {}", authentication.getName(), orderId);
            
            User adminUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            // Update bulk admin response
            List<Review> updatedReviews = reviewService.updateBulkAdminResponse(orderId, adminUser.getId(), request.getAdminResponse());
            
            // Get updated order review group
            Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                log.info("Bulk admin response updated successfully for {} reviews in order {}", updatedReviews.size(), orderId);
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get(), 
                    String.format("Phản hồi đã được cập nhật cho %d đánh giá trong đơn hàng", updatedReviews.size())));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Không thể lấy thông tin đơn hàng sau khi cập nhật"));
            }
            
        } catch (Exception e) {
            log.error("Error updating bulk admin response for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể cập nhật phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Remove bulk admin response from all reviews in an order
     */
    @DeleteMapping("/orders/{orderId}/response")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Xóa phản hồi tất cả đánh giá trong đơn hàng",
        description = "Admin xóa phản hồi cho tất cả đánh giá trong một đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa phản hồi thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> removeBulkAdminResponse(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            log.info("Admin {} removing bulk response from order {}", authentication.getName(), orderId);
            
            User adminUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            // Remove bulk admin response
            List<Review> updatedReviews = reviewService.removeBulkAdminResponse(orderId, adminUser.getId());
            
            // Get updated order review group
            Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                log.info("Bulk admin response removed successfully from {} reviews in order {}", updatedReviews.size(), orderId);
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get(), 
                    String.format("Phản hồi đã được xóa khỏi %d đánh giá trong đơn hàng", updatedReviews.size())));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Không thể lấy thông tin đơn hàng sau khi cập nhật"));
            }
            
        } catch (Exception e) {
            log.error("Error removing bulk admin response from order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể xóa phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Analyze order reviews with AI to get sentiment and suggested replies
     */
    @PostMapping("/orders/{orderId}/analyze")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Phân tích đánh giá đơn hàng với AI",
        description = "Sử dụng AI để phân tích sentiment và gợi ý câu trả lời cho tất cả đánh giá trong đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phân tích AI thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewAiAnalysisResponse>> analyzeOrderReviews(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            log.info("Admin {} analyzing order reviews for order {} with AI", authentication.getName(), orderId);
            
            ReviewAiAnalysisResponse analysis = adminAiInsightsService.analyzeOrderReviews(orderId);
            
            log.info("AI analysis completed for order {}", orderId);
            return ResponseEntity.ok(ResponseWrapper.success(analysis, "Phân tích AI hoàn thành"));
        } catch (IllegalArgumentException e) {
            log.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404)
                    .body(ResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error analyzing order reviews for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ResponseWrapper.error("Có lỗi xảy ra khi phân tích đánh giá: " + e.getMessage()));
        }
    }
}
