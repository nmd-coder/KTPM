package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing product reviews
 * Following rules.mdc specifications for business logic tier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    /**
     * Check if user can review a product (must have completed order with that product)
     */
    public boolean canUserReviewProduct(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }

        log.debug("Checking if user {} can review product {}", userId, productId);
        
        // Check if user has any completed order items for this product
        List<OrderItem> completedOrderItems = orderItemRepository.findCompletedOrderItemsByUserAndProduct(userId, productId, OrderStatus.COMPLETED);
        boolean canReview = !completedOrderItems.isEmpty();
        
        log.debug("User {} can review product {}: {}", userId, productId, canReview);
        return canReview;
    }

    /**
     * Check if user can review a specific order item
     * Returns specific error message if cannot review
     */
    public String canUserReviewOrderItemWithReason(Long userId, Long orderItemId) {
        if (userId == null || orderItemId == null) {
            log.warn("canUserReviewOrderItem: userId or orderItemId is null - userId: {}, orderItemId: {}", userId, orderItemId);
            return "Thông tin không hợp lệ";
        }

        log.info("Checking if user {} can review order item {}", userId, orderItemId);
        
        Optional<OrderItem> orderItemOpt = orderItemRepository.findById(orderItemId);
        if (orderItemOpt.isEmpty()) {
            log.warn("canUserReviewOrderItem: OrderItem {} not found", orderItemId);
            return "Sản phẩm không tồn tại";
        }

        OrderItem orderItem = orderItemOpt.get();
        Order order = orderItem.getOrder();
        
        log.info("OrderItem {}: orderId={}, orderUserId={}, orderStatus={}, currentUserId={}", 
                orderItemId, order.getId(), order.getUser().getId(), order.getStatus(), userId);
        
        // Check if order belongs to user
        if (!order.getUser().getId().equals(userId)) {
            log.warn("canUserReviewOrderItem: Order {} does not belong to user {} (belongs to user {})", 
                    order.getId(), userId, order.getUser().getId());
            return "Đơn hàng không thuộc về bạn";
        }
        
        // Check if order is received (Shopee-like flow: user must confirm received before reviewing)
        if (order.getStatus() != OrderStatus.RECEIVED) {
            log.warn("canUserReviewOrderItem: Order {} status is {} (not RECEIVED)", 
                    order.getId(), order.getStatus());
            return "Vui lòng xác nhận đã nhận hàng trước khi đánh giá";
        }
        
        // Check if already reviewed
        boolean alreadyReviewed = reviewRepository.existsByOrderItemId(orderItemId);
        if (alreadyReviewed) {
            log.warn("canUserReviewOrderItem: OrderItem {} has already been reviewed", orderItemId);
            return "Bạn đã đánh giá sản phẩm này rồi";
        }
        
        log.info("User {} CAN review order item {}", userId, orderItemId);
        return null; // null means can review
    }
    
    /**
     * Check if user can review a specific order item (backward compatibility)
     */
    public boolean canUserReviewOrderItem(Long userId, Long orderItemId) {
        return canUserReviewOrderItemWithReason(userId, orderItemId) == null;
    }

    /**
     * Create a new review
     */
    @Transactional
    public Review createReview(Long userId, Long orderItemId, Integer rating, String comment) {
        if (userId == null || orderItemId == null || rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Invalid review parameters");
        }

        log.info("Creating review for user {} and order item {}", userId, orderItemId);

        // Validate user can review this order item
        String errorReason = canUserReviewOrderItemWithReason(userId, orderItemId);
        if (errorReason != null) {
            throw new IllegalStateException(errorReason);
        }

        // Get order item and related entities
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new IllegalArgumentException("Order item not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Force initialize lazy-loaded entities to prevent LazyInitializationException
        Product product = orderItem.getProduct();
        product.getName(); // Force load
        user.getFirstname(); // Force load

        // Create review
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);
        review.setOrderItem(orderItem);

        Review savedReview = reviewRepository.save(review);
        
        // Force initialize the saved review's lazy-loaded fields
        savedReview.getProduct().getName();
        savedReview.getUser().getFirstname();
        
        log.info("Review created successfully with ID: {}", savedReview.getId());
        
        return savedReview;
    }

    /**
     * Create a new review with media files
     */
    @Transactional
    public Review createReviewWithMedia(Long userId, Long orderItemId, Integer rating, String comment, List<MultipartFile> files) {
        if (userId == null || orderItemId == null || rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Invalid review parameters");
        }

        log.info("Creating review with media for user {} and order item {}", userId, orderItemId);

        // Validate user can review this order item
        String errorReason = canUserReviewOrderItemWithReason(userId, orderItemId);
        if (errorReason != null) {
            throw new IllegalStateException(errorReason);
        }

        // Get order item and related entities
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
            .orElseThrow(() -> new IllegalArgumentException("Order item not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Force initialize lazy-loaded entities to prevent LazyInitializationException
        Product product = orderItem.getProduct();
        product.getName(); // Force load
        user.getFirstname(); // Force load

        // Create review
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);
        review.setOrderItem(orderItem);

        // Handle file uploads
        if (files != null && !files.isEmpty()) {
            String mediaUrls = uploadMediaFiles(files, userId, orderItemId);
            review.setMediaUrls(mediaUrls);
        }

        Review savedReview = reviewRepository.save(review);
        
        // Force initialize the saved review's lazy-loaded fields
        savedReview.getProduct().getName();
        savedReview.getUser().getFirstname();
        
        log.info("Review with media created successfully with ID: {}", savedReview.getId());
        
        return savedReview;
    }

    /**
     * Upload media files and return comma-separated URLs
     */
    private String uploadMediaFiles(List<MultipartFile> files, Long userId, Long orderItemId) {
        if (files == null || files.isEmpty()) {
            return null;
        }

        try {
            // Create upload directory if not exists
            Path uploadDir = Paths.get("src/main/resources/static/upload");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            StringBuilder mediaUrls = new StringBuilder();
            
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                // Validate file type
                String contentType = file.getContentType();
                if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                    log.warn("Skipping file with unsupported content type: {}", contentType);
                    continue;
                }

                // Validate file size (10MB max)
                if (file.getSize() > 10 * 1024 * 1024) {
                    log.warn("Skipping file {} - too large: {} bytes", file.getOriginalFilename(), file.getSize());
                    continue;
                }

                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
                String filename = "review_" + userId + "_" + orderItemId + "_" + UUID.randomUUID().toString() + extension;

                // Save file
                Path filePath = uploadDir.resolve(filename);
                Files.copy(file.getInputStream(), filePath);

                // Add to URLs
                if (mediaUrls.length() > 0) {
                    mediaUrls.append(",");
                }
                mediaUrls.append("/upload/").append(filename);

                log.info("File uploaded successfully: {}", filename);
            }

            return mediaUrls.length() > 0 ? mediaUrls.toString() : null;

        } catch (IOException e) {
            log.error("Error uploading media files: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload media files: " + e.getMessage());
        }
    }

    /**
     * Update an existing review
     */
    @Transactional
    public Review updateReview(Long reviewId, Long userId, Integer rating, String comment) {
        if (reviewId == null || userId == null || rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Invalid review parameters");
        }

        log.info("Updating review {} for user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Check if user owns this review
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User can only update their own reviews");
        }

        // Update review
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : null);

        Review savedReview = reviewRepository.save(review);
        log.info("Review {} updated successfully", reviewId);
        
        return savedReview;
    }


    /**
     * Delete a review (user can delete their own, admin can delete any)
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        if (reviewId == null) {
            throw new IllegalArgumentException("Review ID cannot be null");
        }

        log.info("Deleting review {} by user {} (admin: {})", reviewId, userId, isAdmin);

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Check permissions
        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User can only delete their own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted successfully", reviewId);
    }

    /**
     * Get review by ID with eager loading
     */
    public Review getReviewById(Long reviewId) {
        if (reviewId == null) {
            throw new IllegalArgumentException("Review ID cannot be null");
        }

        log.debug("Getting review by ID: {}", reviewId);
        
        // Use custom query with eager loading to avoid LazyInitializationException
        return reviewRepository.findByIdWithEagerLoading(reviewId)
            .orElseThrow(() -> new RuntimeException("Review not found with ID: " + reviewId));
    }

    /**
     * Get reviews for a product with pagination
     */
    public Page<Review> getReviewsByProduct(Long productId, Pageable pageable) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        log.debug("Getting reviews for product {} with pagination", productId);
        return reviewRepository.findByProductId(productId, pageable);
    }

    /**
     * Get reviews by user
     */
    public List<Review> getReviewsByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.debug("Getting reviews for user {}", userId);
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get user's review for a specific product
     */
    public Optional<Review> getUserReviewForProduct(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return Optional.empty();
        }

        log.debug("Getting user {} review for product {}", userId, productId);
        return reviewRepository.findByUserIdAndProductId(userId, productId);
    }

    /**
     * Get order items that user can review
     * Only orders with RECEIVED status can be reviewed (Shopee-like logic)
     */
    public List<OrderItem> getReviewableOrderItems(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        log.debug("Getting reviewable order items for user {} (RECEIVED status only)", userId);
        return orderItemRepository.findCompletedOrderItemsByUser(userId, OrderStatus.RECEIVED);
    }

    /**
     * Get average rating for a product
     */
    public Double getAverageRating(Long productId) {
        if (productId == null) {
            return 0.0;
        }

        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);
        return averageRating != null ? averageRating : 0.0;
    }

    /**
     * Get review count for a product
     */
    public Long getReviewCount(Long productId) {
        if (productId == null) {
            return 0L;
        }

        Long count = reviewRepository.countReviewsByProductId(productId);
        return count != null ? count : 0L;
    }

    /**
     * Get all reviews with pagination and filters (Admin)
     */
    public Page<Review> getAllReviews(Pageable pageable, Integer rating, String search, String sentiment, String status, String sort) {
        log.debug("Getting all reviews with pagination: {}, rating: {}, search: {}, sentiment: {}, status: {}, sort: {}", 
                  pageable, rating, search, sentiment, status, sort);
        
        // Apply sorting to pageable
        org.springframework.data.domain.Sort sortObj;
        switch (sort != null ? sort : "newest") {
            case "oldest":
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt");
                break;
            case "rating-high":
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rating");
                break;
            case "rating-low":
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "rating");
                break;
            case "newest":
            default:
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
                break;
        }
        
        pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);
        
        // If no filters, use the simple query
        if (rating == null && (search == null || search.trim().isEmpty()) && 
            (sentiment == null || sentiment.trim().isEmpty()) && (status == null || status.trim().isEmpty())) {
            return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        
        // Use filtered query
        return reviewRepository.findAllWithFilters(rating, search, sentiment, status, pageable);
    }
    
    /**
     * Get review statistics (Admin)
     */
    public java.util.Map<String, Object> getReviewStatistics() {
        log.debug("Getting review statistics");
        
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        
        // Total reviews
        long totalReviews = reviewRepository.count();
        statistics.put("totalReviews", totalReviews);
        
        // Average rating
        Double averageRating = reviewRepository.getAverageRating();
        statistics.put("averageRating", averageRating != null ? averageRating : 0.0);
        
        // 5 star reviews
        Long fiveStarReviews = reviewRepository.countByRating(5);
        statistics.put("fiveStarReviews", fiveStarReviews != null ? fiveStarReviews : 0L);
        
        // Today's reviews
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Long todayReviews = reviewRepository.countByCreatedAtAfter(startOfDay);
        statistics.put("todayReviews", todayReviews != null ? todayReviews : 0L);
        
        return statistics;
    }

    /**
     * Add admin response to a review
     */
    @Transactional
    public Review addAdminResponse(Long reviewId, Long adminUserId, String adminResponse) {
        if (reviewId == null || adminUserId == null || adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid admin response parameters");
        }

        log.info("Adding admin response to review {} by admin {}", reviewId, adminUserId);

        // Get review with eager loading
        Review review = reviewRepository.findByIdWithEagerLoading(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Get admin user
        User adminUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        // Update review with admin response
        review.setAdminResponse(adminResponse.trim());
        review.setAdminResponseAt(LocalDateTime.now());
        review.setAdminResponseBy(adminUser);

        Review savedReview = reviewRepository.save(review);
        log.info("Admin response added successfully to review {}", reviewId);
        
        return savedReview;
    }

    /**
     * Update admin response for a review
     */
    @Transactional
    public Review updateAdminResponse(Long reviewId, Long adminUserId, String adminResponse) {
        if (reviewId == null || adminUserId == null || adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid admin response parameters");
        }

        log.info("Updating admin response for review {} by admin {}", reviewId, adminUserId);

        // Get review with eager loading
        Review review = reviewRepository.findByIdWithEagerLoading(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Check if review has admin response
        if (review.getAdminResponse() == null) {
            throw new IllegalStateException("Review does not have an admin response to update");
        }

        // Get admin user
        User adminUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        // Update admin response
        review.setAdminResponse(adminResponse.trim());
        review.setAdminResponseAt(LocalDateTime.now());
        review.setAdminResponseBy(adminUser);

        Review savedReview = reviewRepository.save(review);
        log.info("Admin response updated successfully for review {}", reviewId);
        
        return savedReview;
    }

    /**
     * Remove admin response from a review
     */
    @Transactional
    public Review removeAdminResponse(Long reviewId, Long adminUserId) {
        if (reviewId == null || adminUserId == null) {
            throw new IllegalArgumentException("Review ID and admin user ID cannot be null");
        }

        log.info("Removing admin response from review {} by admin {}", reviewId, adminUserId);

        // Get review with eager loading
        Review review = reviewRepository.findByIdWithEagerLoading(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Check if review has admin response
        if (review.getAdminResponse() == null) {
            throw new IllegalStateException("Review does not have an admin response to remove");
        }

        // Remove admin response
        review.setAdminResponse(null);
        review.setAdminResponseAt(null);
        review.setAdminResponseBy(null);

        Review savedReview = reviewRepository.save(review);
        log.info("Admin response removed successfully from review {}", reviewId);
        
        return savedReview;
    }

    /**
     * Get reviews grouped by order with pagination (Admin)
     */
    public Page<com.example.demo.dto.OrderReviewGroupDTO> getReviewsGroupedByOrder(Pageable pageable, String search, String sort) {
        log.debug("Getting reviews grouped by order with pagination: {}, search: {}, sort: {}", pageable, search, sort);
        
        // Apply sorting to pageable
        org.springframework.data.domain.Sort sortObj;
        switch (sort != null ? sort : "newest") {
            case "oldest":
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "createdAt");
                break;
            case "rating-high":
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "rating");
                break;
            case "rating-low":
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "rating");
                break;
            case "newest":
            default:
                sortObj = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
                break;
        }
        
        pageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortObj);
        
        // Get all reviews with filters
        Page<Review> reviewsPage = reviewRepository.findAllWithFilters(null, search, null, null, pageable);
        
        // Group reviews by order
        java.util.Map<String, java.util.List<Review>> reviewsByOrder = reviewsPage.getContent().stream()
            .filter(review -> review.getOrderItem() != null && review.getOrderItem().getOrder() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                review -> review.getOrderItem().getOrder().getId()
            ));
        
        // Convert to OrderReviewGroupDTO
        java.util.List<com.example.demo.dto.OrderReviewGroupDTO> orderGroups = reviewsByOrder.entrySet().stream()
            .map(entry -> {
                String orderId = entry.getKey();
                java.util.List<Review> orderReviews = entry.getValue();
                
                if (orderReviews.isEmpty()) {
                    return null;
                }
                
                // Get order and customer info from first review
                Review firstReview = orderReviews.get(0);
                com.example.demo.entity.Order order = firstReview.getOrderItem().getOrder();
                com.example.demo.entity.User customer = order.getUser();
                
                // Convert reviews to DTOs
                java.util.List<com.example.demo.dto.ReviewResponse> reviewResponses = orderReviews.stream()
                    .map(review -> new com.example.demo.dto.ReviewResponse(review, false))
                    .collect(java.util.stream.Collectors.toList());
                
                // Create OrderReviewGroupDTO
                return new com.example.demo.dto.OrderReviewGroupDTO(
                    orderId,
                    order.getOrderDate(),
                    customer.getFirstname() + " " + customer.getLastname(),
                    customer.getEmail(),
                    customer.getId(),
                    firstReview.getComment(), // Common comment
                    reviewResponses
                );
            })
            .filter(java.util.Objects::nonNull)
            .sorted((a, b) -> {
                // Apply sorting
                switch (sort != null ? sort : "newest") {
                    case "oldest":
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    case "rating-high":
                        return Double.compare(b.getAverageRating(), a.getAverageRating());
                    case "rating-low":
                        return Double.compare(a.getAverageRating(), b.getAverageRating());
                    case "newest":
                    default:
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                }
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Create pageable result
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), orderGroups.size());
        java.util.List<com.example.demo.dto.OrderReviewGroupDTO> pageContent = orderGroups.subList(start, end);
        
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, orderGroups.size());
    }

    /**
     * Add bulk admin response to all reviews in an order
     */
    @Transactional
    public java.util.List<Review> addBulkAdminResponse(String orderId, Long adminUserId, String adminResponse) {
        if (orderId == null || adminUserId == null || adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid bulk admin response parameters");
        }

        log.info("Adding bulk admin response to order {} by admin {}", orderId, adminUserId);

        // Get all reviews for this order
        java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order not found or has no items");
        }

        // Get admin user
        User adminUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        java.util.List<Review> updatedReviews = new java.util.ArrayList<>();
        
        for (OrderItem orderItem : orderItems) {
            Optional<Review> reviewOpt = reviewRepository.findByOrderItemId(orderItem.getId());
            if (reviewOpt.isPresent()) {
                Review review = reviewOpt.get();
                
                // Update review with admin response
                review.setAdminResponse(adminResponse.trim());
                review.setAdminResponseAt(LocalDateTime.now());
                review.setAdminResponseBy(adminUser);
                
                updatedReviews.add(reviewRepository.save(review));
            }
        }

        log.info("Bulk admin response added successfully to {} reviews in order {}", updatedReviews.size(), orderId);
        return updatedReviews;
    }

    /**
     * Update bulk admin response for all reviews in an order
     */
    @Transactional
    public java.util.List<Review> updateBulkAdminResponse(String orderId, Long adminUserId, String adminResponse) {
        if (orderId == null || adminUserId == null || adminResponse == null || adminResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid bulk admin response parameters");
        }

        log.info("Updating bulk admin response for order {} by admin {}", orderId, adminUserId);

        // Get all reviews for this order
        java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order not found or has no items");
        }

        // Get admin user
        User adminUser = userRepository.findById(adminUserId)
            .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        java.util.List<Review> updatedReviews = new java.util.ArrayList<>();
        
        for (OrderItem orderItem : orderItems) {
            Optional<Review> reviewOpt = reviewRepository.findByOrderItemId(orderItem.getId());
            if (reviewOpt.isPresent()) {
                Review review = reviewOpt.get();
                
                // Check if review has admin response
                if (review.getAdminResponse() == null) {
                    continue; // Skip reviews without admin response
                }
                
                // Update admin response
                review.setAdminResponse(adminResponse.trim());
                review.setAdminResponseAt(LocalDateTime.now());
                review.setAdminResponseBy(adminUser);
                
                updatedReviews.add(reviewRepository.save(review));
            }
        }

        log.info("Bulk admin response updated successfully for {} reviews in order {}", updatedReviews.size(), orderId);
        return updatedReviews;
    }

    /**
     * Remove bulk admin response from all reviews in an order
     */
    @Transactional
    public java.util.List<Review> removeBulkAdminResponse(String orderId, Long adminUserId) {
        if (orderId == null || adminUserId == null) {
            throw new IllegalArgumentException("Order ID and admin user ID cannot be null");
        }

        log.info("Removing bulk admin response from order {} by admin {}", orderId, adminUserId);

        // Get all reviews for this order
        java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("Order not found or has no items");
        }

        java.util.List<Review> updatedReviews = new java.util.ArrayList<>();
        
        for (OrderItem orderItem : orderItems) {
            Optional<Review> reviewOpt = reviewRepository.findByOrderItemId(orderItem.getId());
            if (reviewOpt.isPresent()) {
                Review review = reviewOpt.get();
                
                // Check if review has admin response
                if (review.getAdminResponse() == null) {
                    continue; // Skip reviews without admin response
                }
                
                // Remove admin response
                review.setAdminResponse(null);
                review.setAdminResponseAt(null);
                review.setAdminResponseBy(null);
                
                updatedReviews.add(reviewRepository.save(review));
            }
        }

        log.info("Bulk admin response removed successfully from {} reviews in order {}", updatedReviews.size(), orderId);
        return updatedReviews;
    }

    /**
     * Get order review group by order ID
     */
    public Optional<com.example.demo.dto.OrderReviewGroupDTO> getOrderReviewGroup(String orderId) {
        if (orderId == null) {
            return Optional.empty();
        }

        log.debug("Getting order review group for order {}", orderId);

        // Get all reviews for this order
        java.util.List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            return Optional.empty();
        }

        java.util.List<Review> orderReviews = new java.util.ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            Optional<Review> reviewOpt = reviewRepository.findByOrderItemId(orderItem.getId());
            reviewOpt.ifPresent(orderReviews::add);
        }

        if (orderReviews.isEmpty()) {
            return Optional.empty();
        }

        // Get order and customer info from first review
        Review firstReview = orderReviews.get(0);
        com.example.demo.entity.Order order = firstReview.getOrderItem().getOrder();
        com.example.demo.entity.User customer = order.getUser();

        // Convert reviews to DTOs
        java.util.List<com.example.demo.dto.ReviewResponse> reviewResponses = orderReviews.stream()
            .map(review -> new com.example.demo.dto.ReviewResponse(review, false))
            .collect(java.util.stream.Collectors.toList());

        // Create OrderReviewGroupDTO
        com.example.demo.dto.OrderReviewGroupDTO orderGroup = new com.example.demo.dto.OrderReviewGroupDTO(
            orderId,
            order.getOrderDate(),
            customer.getFirstname() + " " + customer.getLastname(),
            customer.getEmail(),
            customer.getId(),
            firstReview.getComment(), // Common comment
            reviewResponses
        );

        return Optional.of(orderGroup);
    }
    
    /**
     * Get reviews by order ID
     * Returns list of ReviewResponse for all reviews in the order
     */
    public List<com.example.demo.dto.ReviewResponse> getReviewsByOrderId(String orderId) {
        log.debug("Getting reviews for order: {}", orderId);
        
        try {
            List<Review> reviews = reviewRepository.findByOrderId(orderId);
            
            return reviews.stream()
                .map(review -> {
                    com.example.demo.dto.ReviewResponse response = new com.example.demo.dto.ReviewResponse();
                    response.setId(review.getId());
                    response.setRating(review.getRating());
                    response.setComment(review.getComment());
                    response.setCreatedAt(review.getCreatedAt());
                    
                    // Product info
                    if (review.getProduct() != null) {
                        response.setProductId(review.getProduct().getId());
                        response.setProductName(review.getProduct().getName());
                        response.setProductImage(review.getProduct().getImage());
                    }
                    
                    // User info
                    if (review.getUser() != null) {
                        response.setUserId(review.getUser().getId());
                        response.setUserName(review.getUser().getFullName());
                    }
                    
                    // Admin response
                    response.setAdminResponse(review.getAdminResponse());
                    response.setAdminResponseAt(review.getAdminResponseAt());
                    if (review.getAdminResponseBy() != null) {
                        response.setAdminResponseByName(review.getAdminResponseBy().getFullName());
                    }
                    
                    return response;
                })
                .toList();
                
        } catch (Exception e) {
            log.error("Error getting reviews for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy đánh giá đơn hàng: " + e.getMessage());
        }
    }
}
