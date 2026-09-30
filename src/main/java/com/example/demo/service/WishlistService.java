package com.example.demo.service;

import com.example.demo.dto.WishlistDTO;
import com.example.demo.dto.WishlistResponse;
import com.example.demo.entity.Follow;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.Set; // Thêm import này ở đầu tệp
import java.util.Collections; // Thêm import này ở đầu tệp

/**
 * Service class for managing user wishlist (Follow entity)
 * Handles business logic for wishlist products functionality
 */
@Service
@Transactional
public class WishlistService {

    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Add a product to user's wishlist
     */
    public WishlistResponse addToWishlist(Long userId, Long productId) {
        try {
            logger.info("Adding product {} to wishlist for user {}", productId, userId);

            // Check if already exists
            if (followRepository.existsByUserIdAndProductId(userId, productId)) {
                return WishlistResponse.error("Sản phẩm đã có trong danh sách yêu thích");
            }

            // Validate user exists
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return WishlistResponse.error("Người dùng không tồn tại");
            }

            // Validate product exists
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return WishlistResponse.error("Sản phẩm không tồn tại");
            }

            // Create new follow
            Follow follow = new Follow(userOpt.get(), productOpt.get());
            followRepository.save(follow);

            Long productFollowersCount = followRepository.countFollowersByProductId(productId);
            Long userWishlistCount = followRepository.countByUserId(userId);

            logger.info("Successfully added product {} to wishlist for user {}", productId, userId);
            WishlistResponse response = WishlistResponse.success("Đã thêm vào danh sách yêu thích", true,
                    productFollowersCount);
            response.setUserWishlistCount(userWishlistCount);
            return response;

        } catch (Exception e) {
            logger.error("Error adding product {} to wishlist for user {}: {}", productId, userId, e.getMessage());
            return WishlistResponse.error("Có lỗi xảy ra khi thêm vào danh sách yêu thích");
        }
    }

    /**
     * Remove a product from user's wishlist
     */
    public WishlistResponse removeFromWishlist(Long userId, Long productId) {
        try {
            logger.info("Removing product {} from wishlist for user {}", productId, userId);

            // Check if exists
            if (!followRepository.existsByUserIdAndProductId(userId, productId)) {
                return WishlistResponse.error("Sản phẩm không có trong danh sách yêu thích");
            }

            // tính trước khi xóa , fix lỗi 1 xóa vẫn còn 1
            Long initialUserWishlistCount = followRepository.countByUserId(userId);

            // Remove from wishlist
            followRepository.deleteByUserIdAndProductId(userId, productId);

            Long productFollowersCount = followRepository.countFollowersByProductId(productId);
            Long userWishlistCount = initialUserWishlistCount > 0 ? initialUserWishlistCount - 1 : 0;
            
            logger.info("Successfully removed product {} from wishlist for user {}", productId, userId);
            WishlistResponse response = WishlistResponse.success("Đã xóa khỏi danh sách yêu thích", false,
                    productFollowersCount);
            response.setUserWishlistCount(userWishlistCount);
            return response;

        } catch (Exception e) {
            logger.error("Error removing product {} from wishlist for user {}: {}", productId, userId, e.getMessage());
            return WishlistResponse.error("Có lỗi xảy ra khi xóa khỏi danh sách yêu thích");
        }
    }

    /**
     * Toggle wishlist status of a product for user
     */
    public WishlistResponse toggleWishlist(Long userId, Long productId) {
        boolean isInWishlist = followRepository.existsByUserIdAndProductId(userId, productId);
        logger.info("Toggle wishlist - User: {}, Product: {}, Current status: {}", userId, productId, isInWishlist);

        if (isInWishlist) {
            logger.info("Removing from wishlist - User: {}, Product: {}", userId, productId);
            return removeFromWishlist(userId, productId);
        } else {
            logger.info("Adding to wishlist - User: {}, Product: {}", userId, productId);
            return addToWishlist(userId, productId);
        }
    }

    /**
     * Check if a product is in user's wishlist
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return followRepository.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Get user's wishlist products with pagination
     */
    @Transactional(readOnly = true)
    public Page<WishlistDTO> getUserWishlist(Long userId, Pageable pageable) {
        try {
            logger.info("Getting wishlist for user {} with pagination", userId);

            List<Follow> follows = followRepository.findByUserId(userId);

            List<WishlistDTO> wishlistDTOs = follows.stream()
                    .map(follow -> WishlistDTO.fromProduct(
                            follow.getProduct(),
                            userId,
                            follow.getId(),
                            follow.getFollowedAt()))
                    .collect(Collectors.toList());

            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), wishlistDTOs.size());

            List<WishlistDTO> pageContent = wishlistDTOs.subList(start, end);

            return new PageImpl<>(pageContent, pageable, wishlistDTOs.size());

        } catch (Exception e) {
            logger.error("Error getting wishlist for user {}: {}", userId, e.getMessage());
            return Page.empty(pageable);
        }
    }

    /**
     * Get user's wishlist products without pagination
     */
    @Transactional(readOnly = true)
    public List<WishlistDTO> getUserWishlist(Long userId) {
        try {
            logger.info("Getting all wishlist for user {}", userId);

            List<Follow> follows = followRepository.findByUserId(userId);

            return follows.stream()
                    .map(follow -> WishlistDTO.fromProduct(
                            follow.getProduct(),
                            userId,
                            follow.getId(),
                            follow.getFollowedAt()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error getting wishlist for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get wishlist status for a product
     */
    @Transactional(readOnly = true)
    public WishlistResponse getWishlistStatus(Long userId, Long productId) {
        try {
            boolean isInWishlist = followRepository.existsByUserIdAndProductId(userId, productId);
            Long wishlistCount = followRepository.countFollowersByProductId(productId);

            return WishlistResponse.success("Trạng thái wishlist", isInWishlist, wishlistCount);

        } catch (Exception e) {
            logger.error("Error getting wishlist status for user {} and product {}: {}", userId, productId,
                    e.getMessage());
            return WishlistResponse.error("Có lỗi xảy ra khi lấy trạng thái wishlist");
        }
    }

    /**
     * Get user's wishlist count
     */
    @Transactional(readOnly = true)
    public Long getUserWishlistCount(Long userId) {
        try {
            return followRepository.countByUserId(userId);
        } catch (Exception e) {
            logger.error("Error getting wishlist count for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }

    /**
     * Clear all wishlist items for user
     */
    public WishlistResponse clearUserWishlist(Long userId) {
        try {
            logger.info("Clearing all wishlist items for user {}", userId);

            List<Follow> userFollows = followRepository.findByUserId(userId);
            if (userFollows.isEmpty()) {
                return WishlistResponse.error("Danh sách yêu thích đã trống");
            }

            // Delete all follows for this user
            followRepository.deleteAll(userFollows);

            logger.info("Successfully cleared all wishlist items for user {}", userId);
            WishlistResponse response = WishlistResponse.success("Đã xóa tất cả sản phẩm khỏi danh sách yêu thích", false, 0L);
            response.setUserWishlistCount(0L);
            return response;

        } catch (Exception e) {
            logger.error("Error clearing wishlist for user {}: {}", userId, e.getMessage());
            return WishlistResponse.error("Có lỗi xảy ra khi xóa danh sách yêu thích");
        }
    }

    /**
     * Get a set of product IDs from user's wishlist
     * This is optimized for checking existence in the UI
     * 
     * @param userId The ID of the user
     * @return A Set of product IDs
     */
    @Transactional(readOnly = true)
    public Set<Long> getWishlistProductIds(Long userId) {
        try {
            // Chúng ta sẽ tạo phương thức findProductIdsByUserId ở bước tiếp theo
            return followRepository.findProductIdsByUserId(userId);
        } catch (Exception e) {
            logger.error("Error getting wishlist product IDs for user {}: {}", userId, e.getMessage());
            return Collections.emptySet(); // Trả về một Set rỗng nếu có lỗi
        }
    }
}
