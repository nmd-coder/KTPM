package com.example.demo.repository;

import com.example.demo.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Page<Review> findByProductId(Long productId, Pageable pageable);
    
    /**
     * Find reviews by product ID with eager loading of user and admin data
     */
    @Query(value = "SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.product " +
           "LEFT JOIN FETCH r.orderItem " +
           "LEFT JOIN FETCH r.adminResponseBy " +
           "WHERE r.product.id = :productId " +
           "ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Page<Review> findByProductIdWithUser(@Param("productId") Long productId, Pageable pageable);
    
    List<Review> findByUserId(Long userId);
    
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countReviewsByProductId(@Param("productId") Long productId);
    
    @Query("SELECT r FROM Review r WHERE r.rating = :rating")
    List<Review> findByRating(@Param("rating") Integer rating);
    
    /**
     * Find review by order item ID
     */
    Optional<Review> findByOrderItemId(Long orderItemId);
    
    /**
     * Check if review exists for order item
     */
    boolean existsByOrderItemId(Long orderItemId);
    
    /**
     * Find reviews by user ordered by creation date descending
     */
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find all reviews ordered by creation date descending (for admin)
     * Using LEFT JOIN FETCH to eagerly load related entities
     */
    @Query(value = "SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.product " +
           "LEFT JOIN FETCH r.orderItem " +
           "LEFT JOIN FETCH r.adminResponseBy " +
           "ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Review r")
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Count reviews with rating >= threshold created after date (for AI insights)
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating >= :rating AND r.createdAt >= :date")
    Long countByRatingGreaterThanEqualAndCreatedAtAfter(@Param("rating") Integer rating, @Param("date") java.time.LocalDateTime date);
    
    /**
     * Count total reviews created after date (for AI insights)
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.createdAt >= :date")
    Long countByCreatedAtAfter(@Param("date") java.time.LocalDateTime date);
    
    /**
     * Get average rating for all reviews
     */
    @Query("SELECT AVG(r.rating) FROM Review r")
    Double getAverageRating();
    
    /**
     * Count reviews by rating
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating = :rating")
    Long countByRating(@Param("rating") Integer rating);
    
    /**
     * Find review by ID with eager loading of all relationships
     */
    @Query("SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.product " +
           "LEFT JOIN FETCH r.orderItem " +
           "LEFT JOIN FETCH r.adminResponseBy " +
           "WHERE r.id = :id")
    Optional<Review> findByIdWithEagerLoading(@Param("id") Long id);

    /**
     * Find all reviews with filters (Admin)
     */
    @Query(value = "SELECT DISTINCT r FROM Review r " +
           "LEFT JOIN FETCH r.user u " +
           "LEFT JOIN FETCH r.product p " +
           "LEFT JOIN FETCH r.orderItem oi " +
           "LEFT JOIN FETCH r.adminResponseBy arb " +
           "WHERE (:rating IS NULL OR r.rating = :rating) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(u.firstname) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(u.lastname) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:sentiment IS NULL OR :sentiment = '' OR r.sentiment = :sentiment) " +
           "AND (:status IS NULL OR :status = '' OR " +
           "     (:status = 'verified' AND r.orderItem IS NOT NULL) OR " +
           "     (:status = 'unverified' AND r.orderItem IS NULL))",
           countQuery = "SELECT COUNT(r) FROM Review r " +
           "LEFT JOIN r.user u " +
           "LEFT JOIN r.product p " +
           "WHERE (:rating IS NULL OR r.rating = :rating) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(u.firstname) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(u.lastname) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:sentiment IS NULL OR :sentiment = '' OR r.sentiment = :sentiment) " +
           "AND (:status IS NULL OR :status = '' OR " +
           "     (:status = 'verified' AND r.orderItem IS NOT NULL) OR " +
           "     (:status = 'unverified' AND r.orderItem IS NULL))")
    Page<Review> findAllWithFilters(@Param("rating") Integer rating,
                                     @Param("search") String search,
                                     @Param("sentiment") String sentiment,
                                     @Param("status") String status,
                                     Pageable pageable);

    /**
     * Find reviews by order ID through order items
     */
    @Query("SELECT r FROM Review r " +
           "LEFT JOIN FETCH r.user " +
           "LEFT JOIN FETCH r.product " +
           "LEFT JOIN FETCH r.orderItem oi " +
           "LEFT JOIN FETCH r.adminResponseBy " +
           "WHERE oi.order.id = :orderId " +
           "ORDER BY r.createdAt DESC")
    List<Review> findByOrderId(@Param("orderId") String orderId);
    
    /**
     * Check if order has any reviews
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r " +
           "JOIN r.orderItem oi " +
           "WHERE oi.order.id = :orderId")
    boolean existsByOrderItemOrderId(@Param("orderId") String orderId);
}
