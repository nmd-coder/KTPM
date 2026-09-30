package com.example.demo.dto;

import com.example.demo.entity.Product;
import com.example.demo.entity.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Product in Admin API responses
 * Avoids lazy loading issues by explicitly mapping only needed fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductDTO {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String image;
    private Integer stockQuantity;
    private ProductStatus status;
    private Integer weightG;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;
    private Long catalogId;
    private String catalogValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double averageRating;
    private Long reviewCount;
    
    /**
     * Create DTO from Product entity
     */
    public static AdminProductDTO fromEntity(Product product) {
        AdminProductDTO dto = new AdminProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImage(product.getImage());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setStatus(product.getStatus());
        dto.setWeightG(product.getWeightG());
        dto.setLengthCm(product.getLengthCm());
        dto.setWidthCm(product.getWidthCm());
        dto.setHeightCm(product.getHeightCm());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        
        // Safely get catalog info
        if (product.getCatalog() != null) {
            dto.setCatalogId(product.getCatalog().getId());
            dto.setCatalogValue(product.getCatalog().getValue());
        }
        
        return dto;
    }
    
    /**
     * Create DTO from Product entity with rating info
     */
    public static AdminProductDTO fromEntityWithRating(Product product, Double averageRating, Long reviewCount) {
        AdminProductDTO dto = fromEntity(product);
        dto.setAverageRating(averageRating != null ? averageRating : 0.0);
        dto.setReviewCount(reviewCount != null ? reviewCount : 0L);
        return dto;
    }
}

