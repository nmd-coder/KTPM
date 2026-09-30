package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for wishlist operations
 */
public class WishlistResponse {
    
    private boolean success;
    private String message;
    private boolean isInWishlist;
    private Long wishlistCount; // Count of followers for this specific product
    private Long userWishlistCount; // Total wishlist count for the user
    
    // Constructors
    public WishlistResponse() {
    }
    
    public WishlistResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public WishlistResponse(boolean success, String message, boolean isInWishlist) {
        this.success = success;
        this.message = message;
        this.isInWishlist = isInWishlist;
    }
    
    public WishlistResponse(boolean success, String message, boolean isInWishlist, Long wishlistCount) {
        this.success = success;
        this.message = message;
        this.isInWishlist = isInWishlist;
        this.wishlistCount = wishlistCount;
    }
    
    // Static factory methods
    public static WishlistResponse success(String message) {
        return new WishlistResponse(true, message);
    }
    
    public static WishlistResponse success(String message, boolean isInWishlist) {
        return new WishlistResponse(true, message, isInWishlist);
    }
    
    public static WishlistResponse success(String message, boolean isInWishlist, Long wishlistCount) {
        return new WishlistResponse(true, message, isInWishlist, wishlistCount);
    }
    
    public static WishlistResponse error(String message) {
        return new WishlistResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isInWishlist() {
        return isInWishlist;
    }
    
    public void setInWishlist(boolean inWishlist) {
        isInWishlist = inWishlist;
    }
    
    public Long getWishlistCount() {
        return wishlistCount;
    }
    
    public void setWishlistCount(Long wishlistCount) {
        this.wishlistCount = wishlistCount;
    }
    
    public Long getUserWishlistCount() {
        return userWishlistCount;
    }
    
    public void setUserWishlistCount(Long userWishlistCount) {
        this.userWishlistCount = userWishlistCount;
    }
    
    // For backward compatibility with frontend
    @JsonProperty("isFavorite")
    public boolean isFavorite() {
        return isInWishlist;
    }
    
    public void setFavorite(boolean favorite) {
        isInWishlist = favorite;
    }
    
    @JsonProperty("favoriteCount")
    public Long getFavoriteCount() {
        return wishlistCount;
    }
    
    public void setFavoriteCount(Long favoriteCount) {
        this.wishlistCount = favoriteCount;
    }
}
