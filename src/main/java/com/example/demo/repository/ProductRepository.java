package com.example.demo.repository;

import com.example.demo.entity.Product;
import com.example.demo.entity.enums.ProductStatus;
import com.example.demo.dto.ProductDetailDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
//     @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
//     Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    

    @Query("SELECT p FROM Product p LEFT JOIN p.catalog c WHERE " +
                     "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                     "LOWER(c.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
       Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    // Enhanced search for AI - includes catalog
    @Query("SELECT p FROM Product p LEFT JOIN p.catalog c WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchProductsForAi(@Param("keyword") String keyword, Pageable pageable);

    // Combined filter: Catalog ID AND Search keyword
       @Query("SELECT p FROM Product p WHERE " +
                     "(:catalogId IS NULL OR p.catalog.id = :catalogId) AND " +
                     "(:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")

       Page<Product> findByCatalogIdAndNameContainingIgnoreCase(
               @Param("catalogId") Long catalogId, 
               @Param("keyword") String keyword, 
               Pageable pageable);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.reviews")
    List<Product> findAllWithReviews();
    
    @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
    List<Product> findLatestProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p LEFT JOIN p.orderItems oi GROUP BY p ORDER BY COUNT(oi) DESC")
    List<Product> findBestSellingProducts(Pageable pageable);
    
    // Query by catalog
    List<Product> findByCatalogId(Long catalogId);
    
    @RestResource(path = "findByCatalogIdPaged", rel = "findByCatalogIdPaged")
    Page<Product> findByCatalogId(Long catalogId, Pageable pageable);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.catalog WHERE p.id = :productId")
    Optional<Product> findByIdWithCatalog(@Param("productId") Long productId);
    
    @Query("SELECT new com.example.demo.dto.ProductDetailDTO(p, AVG(r.rating), COUNT(r)) " +
           "FROM Product p LEFT JOIN p.reviews r WHERE p.id = :productId GROUP BY p")
    Optional<ProductDetailDTO> findProductDetailById(@Param("productId") Long productId);
    
    // Admin methods
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") ProductStatus status);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity < :threshold")
    long countByStockQuantityLessThan(@Param("threshold") Integer threshold);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity > 0 AND p.stockQuantity < :threshold")
    long countByStockQuantityBetweenZeroAnd(@Param("threshold") Integer threshold);
    
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0")
    long countByStockQuantityEquals0();
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.reviews WHERE p.id = :productId")
    Optional<Product> findByIdWithReviews(@Param("productId") Long productId);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.catalog WHERE p.id = :productId")
    Optional<Product> findByIdWithCatalogEager(@Param("productId") Long productId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long getReviewCountByProductId(@Param("productId") Long productId);
    
    // AI Insights queries
    @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold ORDER BY p.stockQuantity ASC")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > :threshold ORDER BY p.stockQuantity DESC")
    List<Product> findHighStockProducts(@Param("threshold") Integer threshold);
    
    @Query(value = "SELECT p.name, COUNT(oi.id) as order_count " +
           "FROM products p " +
           "LEFT JOIN orderitems oi ON p.id = oi.product_id " +
           "LEFT JOIN orders o ON oi.order_id = o.id " +
           "WHERE o.order_date >= CURRENT_DATE - INTERVAL '7 days' " +
           "GROUP BY p.id, p.name " +
           "ORDER BY order_count DESC " +
           "LIMIT 3", nativeQuery = true)
    List<Object[]> findTopSellingProductsLast7Days();
    
    /**
     * Find products with high stock but low sales in the last 30 days
     * Used for AI inventory clearance suggestions
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.stockQuantity > 50 " +  // High stock threshold
           "AND p.id NOT IN (" +
           "    SELECT DISTINCT oi.product.id FROM OrderItem oi " +
           "    JOIN oi.order o " +
           "    WHERE o.orderDate >= :thirtyDaysAgo " +
           "    AND o.status = 'COMPLETED'" +
           ") " +
           "ORDER BY p.stockQuantity DESC")
    List<Product> findHighStockLowSalesProducts(@Param("thirtyDaysAgo") java.time.LocalDateTime thirtyDaysAgo);
    
    /**
     * Find products by name for specific inventory clearance
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%'))")
    List<Product> findProductsByName(@Param("productName") String productName);
}
