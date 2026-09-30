package com.example.demo.entity;

import com.example.demo.entity.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column
    private String image;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private ProductStatus status = ProductStatus.ACTIVE;

    // GHN Shipping Dimensions (for shipping fee calculation)
    @Column(name = "weight_g")
    private Integer weightG = 500; // Default 500g

    @Column(name = "length_cm")
    private Integer lengthCm = 20; // Default 20cm

    @Column(name = "width_cm")
    private Integer widthCm = 20; // Default 20cm

    @Column(name = "height_cm")
    private Integer heightCm = 30; // Default 30cm

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_id")
    private Catalog catalog;

    // Relationships

    @JsonIgnore
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<CartItem> cartItems = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Follow> follows = new ArrayList<>();

    // Constructors
    public Product() {
    }

    public Product(String name, String description, BigDecimal price, String image) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
        // Set default shipping dimensions
        this.weightG = 500;
        this.lengthCm = 20;
        this.widthCm = 20;
        this.heightCm = 30;
    }

    // Helper methods
    public Double getAverageRating() {
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    public boolean isLowStock(int threshold) {
        return stockQuantity != null && stockQuantity <= threshold;
    }

    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && isInStock();
    }

    public void updateStatusBasedOnStock() {
        if (stockQuantity == null || stockQuantity <= 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        } else if (status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.ACTIVE;
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public List<Follow> getFollows() {
        return follows;
    }

    public void setFollows(List<Follow> follows) {
        this.follows = follows;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        updateStatusBasedOnStock();
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    // GHN Shipping Dimensions getters and setters
    public Integer getWeightG() {
        return weightG;
    }

    public void setWeightG(Integer weightG) {
        this.weightG = weightG;
    }

    public Integer getLengthCm() {
        return lengthCm;
    }

    public void setLengthCm(Integer lengthCm) {
        this.lengthCm = lengthCm;
    }

    public Integer getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(Integer widthCm) {
        this.widthCm = widthCm;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    // Helper method for total shipping weight calculation
    public int getTotalWeightForQuantity(int quantity) {
        return (weightG != null ? weightG : 500) * quantity;
    }

    // Helper method to get max dimensions (for multiple items)
    public int getMaxLength() {
        return lengthCm != null ? lengthCm : 20;
    }

    public int getMaxWidth() {
        return widthCm != null ? widthCm : 20;
    }

    public int getMaxHeight() {
        return heightCm != null ? heightCm : 30;
    }
}
