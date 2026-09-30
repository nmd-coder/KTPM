package com.example.demo.dto;

import com.example.demo.entity.User;

/**
 * DTO for optimized user profile queries
 * Contains user information along with aggregated statistics
 */
public class UserProfileDTO {
    private final User user;
    private final Long ordersCount;
    private final Long wishlistCount;
    private final Long cartItemsCount;

    public UserProfileDTO(User user, Long ordersCount, Long wishlistCount, Long cartItemsCount) {
        this.user = user;
        this.ordersCount = ordersCount;
        this.wishlistCount = wishlistCount;
        this.cartItemsCount = cartItemsCount;
    }

    public User getUser() {
        return user;
    }

    public Long getOrdersCount() {
        return ordersCount;
    }

    public Long getWishlistCount() {
        return wishlistCount;
    }

    public Long getCartItemsCount() {
        return cartItemsCount;
    }
}
