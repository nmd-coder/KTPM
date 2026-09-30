package com.example.demo.controller;

import com.example.demo.dto.ResponseWrapper;
import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.dto.ReviewableItemDTO;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API Controller for Review Operations
 * Following rules.mdc specifications for REST API
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "⭐ Reviews", description = "Review APIs - Create, read, update, and delete product reviews")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    /**
     * Create a new review
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Tạo đánh giá mới",
        description = "Tạo đánh giá cho sản phẩm sau khi đơn hàng hoàn thành"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tạo đánh giá thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền tạo đánh giá")
    })
    public ResponseEntity<ResponseWrapper<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        
        try {
            log.info("Creating review for order item {} by user {}", 
                    request.getOrderItemId(), authentication.getName());
            
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Review review = reviewService.createReview(
                user.getId(), 
                request.getOrderItemId(), 
                request.getRating(), 
                request.getComment()
            );
            
            ReviewResponse response = new ReviewResponse(review, true);
            
            log.info("Review created successfully with ID: {}", review.getId());
            return ResponseEntity.ok(ResponseWrapper.success(response, "Đánh giá đã được tạo thành công"));
            
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể tạo đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Update an existing review
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Cập nhật đánh giá",
        description = "Cập nhật đánh giá của người dùng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật đánh giá thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật đánh giá")
    })
    public ResponseEntity<ResponseWrapper<ReviewResponse>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        
        try {
            log.info("Updating review {} by user {}", id, authentication.getName());
            
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Review review = reviewService.updateReview(
                id, 
                user.getId(), 
                request.getRating(), 
                request.getComment()
            );
            
            ReviewResponse response = new ReviewResponse(review, true);
            
            log.info("Review {} updated successfully", id);
            return ResponseEntity.ok(ResponseWrapper.success(response, "Đánh giá đã được cập nhật thành công"));
            
        } catch (Exception e) {
            log.error("Error updating review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể cập nhật đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Delete a review
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @Operation(
        summary = "Xóa đánh giá",
        description = "Xóa đánh giá (người dùng chỉ có thể xóa đánh giá của mình, admin có thể xóa bất kỳ)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa đánh giá thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền xóa đánh giá")
    })
    public ResponseEntity<ResponseWrapper<String>> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            log.info("Deleting review {} by user {}", id, authentication.getName());
            
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean isAdmin = user.getRole().name().equals("ADMIN");
            
            reviewService.deleteReview(id, user.getId(), isAdmin);
            
            log.info("Review {} deleted successfully", id);
            return ResponseEntity.ok(ResponseWrapper.success("Đánh giá đã được xóa thành công"));
            
        } catch (Exception e) {
            log.error("Error deleting review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể xóa đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Get reviews for a product
     */
    @GetMapping("/product/{productId}")
    @Operation(
        summary = "Lấy đánh giá sản phẩm",
        description = "Lấy danh sách đánh giá của sản phẩm với phân trang"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy đánh giá thành công")
    })
    public ResponseEntity<ResponseWrapper<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số đánh giá mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        try {
            log.info("Getting reviews for product {} with pagination: page={}, size={}", 
                    productId, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Review> reviewsPage = reviewService.getReviewsByProduct(productId, pageable);
            
            // Convert to DTOs
            Page<ReviewResponse> responsePage = reviewsPage.map(review -> {
                boolean canEdit = false;
                if (authentication != null && authentication.isAuthenticated()) {
                    User user = userRepository.findByEmail(authentication.getName()).orElse(null);
                    canEdit = user != null && user.getId().equals(review.getUser().getId());
                }
                return new ReviewResponse(review, canEdit);
            });
            
            return ResponseEntity.ok(ResponseWrapper.success(responsePage));
            
        } catch (Exception e) {
            log.error("Error getting reviews for product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Get current user's reviews
     */
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Lấy đánh giá của tôi",
        description = "Lấy danh sách đánh giá của người dùng hiện tại"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy đánh giá thành công")
    })
    public ResponseEntity<ResponseWrapper<List<ReviewResponse>>> getMyReviews(
            Authentication authentication) {
        
        try {
            log.info("Getting reviews for user {}", authentication.getName());
            
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Review> reviews = reviewService.getReviewsByUser(user.getId());
            List<ReviewResponse> responses = reviews.stream()
                .map(review -> new ReviewResponse(review, true))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ResponseWrapper.success(responses));
            
        } catch (Exception e) {
            log.error("Error getting user reviews: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Get reviewable order items for current user
     */
    @GetMapping("/reviewable-items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Lấy sản phẩm có thể đánh giá",
        description = "Lấy danh sách sản phẩm từ đơn hàng hoàn thành mà người dùng có thể đánh giá"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public ResponseEntity<ResponseWrapper<List<ReviewableItemDTO>>> getReviewableItems(
            Authentication authentication) {
        
        try {
            log.info("Getting reviewable items for user {}", authentication.getName());
            
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<ReviewableItemDTO> reviewableItems = reviewService.getReviewableOrderItems(user.getId())
                .stream()
                .map(orderItem -> {
                    boolean alreadyReviewed = reviewService.getUserReviewForProduct(
                        user.getId(), orderItem.getProduct().getId()).isPresent();
                    return new ReviewableItemDTO(orderItem, alreadyReviewed);
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ResponseWrapper.success(reviewableItems));
            
        } catch (Exception e) {
            log.error("Error getting reviewable items: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy danh sách: " + e.getMessage()));
        }
    }

    /**
     * Check if user can review a specific order item
     */
    @GetMapping("/can-review/{productId}/{orderItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Kiểm tra có thể đánh giá",
        description = "Kiểm tra người dùng có thể đánh giá sản phẩm từ order item cụ thể không"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kiểm tra thành công")
    })
    public ResponseEntity<ResponseWrapper<Boolean>> canReview(
            @PathVariable Long productId,
            @PathVariable Long orderItemId,
            Authentication authentication) {
        
        try {
            log.info("Checking if user {} can review product {} via order item {}", 
                    authentication.getName(), productId, orderItemId);
            
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            boolean canReview = reviewService.canUserReviewOrderItem(user.getId(), orderItemId);
            
            return ResponseEntity.ok(ResponseWrapper.success(canReview));
            
        } catch (Exception e) {
            log.error("Error checking review permission: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể kiểm tra quyền: " + e.getMessage()));
        }
    }

    
    /**
     * Tạo đánh giá cho một sản phẩm cụ thể trong đơn hàng
     */
    @PostMapping(value = "/order-item/{orderItemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo đánh giá cho một sản phẩm cụ thể")
    public ResponseEntity<ResponseWrapper<ReviewResponse>> createOrderItemReview(
            @PathVariable Long orderItemId,
            @RequestParam("rating") Integer rating,
            @RequestParam("comment") String comment,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            com.example.demo.entity.Review review = reviewService.createReviewWithMedia(
                user.getId(), orderItemId, rating, comment, files);

            ReviewResponse reviewResponse = new ReviewResponse(review, true);
            return ResponseEntity.ok(ResponseWrapper.success(reviewResponse, "Đánh giá sản phẩm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể tạo đánh giá: " + e.getMessage()));
        }
    }
}
