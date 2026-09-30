package com.example.demo.service;

import com.example.demo.dto.CartDTO;
import com.example.demo.dto.CartResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for managing user shopping cart
 * Handles business logic for cart functionality
 */
@Service
@Transactional
public class CartService {
    
    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get or create user's cart
     */
    public Cart getOrCreateCart(Long userId) {
        Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
        
        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }
        
        // Create new cart for user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        Cart cart = new Cart(userOpt.get());
        return cartRepository.save(cart);
    }
    
    /**
     * Add product to cart
     */
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        try {
            logger.info("Adding product {} to cart for user {} with quantity {}", productId, userId, quantity);
            
            // Validate product exists and is available
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                return CartResponse.error("Sản phẩm không tồn tại");
            }
            
            Product product = productOpt.get();
            if (product.getStockQuantity() < quantity) {
                return CartResponse.error("Không đủ hàng trong kho");
            }
            
            // Get or create cart
            Cart cart = getOrCreateCart(userId);
            
            // Check if item already exists in cart
            Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
            
            if (existingItemOpt.isPresent()) {
                // Update existing item
                CartItem existingItem = existingItemOpt.get();
                int newQuantity = existingItem.getQuantity() + quantity;
                
                if (product.getStockQuantity() < newQuantity) {
                    return CartResponse.error("Không đủ hàng trong kho");
                }
                
                existingItem.setQuantity(newQuantity);
                cartItemRepository.save(existingItem);
            } else {
                // Create new cart item
                CartItem cartItem = new CartItem(cart, product, quantity);
                cartItemRepository.save(cartItem);
                cart.addItem(cartItem);
            }
            
            // Recalculate cart totals
            cart.calculateTotalAmount();
            cartRepository.save(cart);
            
            // Get updated cart count
            Long totalItems = cartRepository.countItemsByUserId(userId);
            
            logger.info("Successfully added product {} to cart for user {}", productId, userId);
            return CartResponse.success("Đã thêm sản phẩm vào giỏ hàng", CartDTO.fromCart(cart), totalItems);
            
        } catch (Exception e) {
            logger.error("Error adding product {} to cart for user {}: {}", productId, userId, e.getMessage());
            return CartResponse.error("Có lỗi xảy ra khi thêm sản phẩm vào giỏ hàng");
        }
    }
    
    /**
     * Update cart item quantity
     */
    public CartResponse updateCartItem(Long userId, Long productId, Integer quantity) {
        try {
            logger.info("Updating cart item for user {} - product {} to quantity {}", userId, productId, quantity);
            
            Cart cart = getOrCreateCart(userId);
            Optional<CartItem> cartItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
            
            if (cartItemOpt.isEmpty()) {
                return CartResponse.error("Sản phẩm không có trong giỏ hàng");
            }
            
            CartItem cartItem = cartItemOpt.get();
            
            if (quantity <= 0) {
                // Remove item if quantity is 0 or negative
                return removeFromCart(userId, productId);
            }
            
            // Check stock availability
            if (cartItem.getProduct().getStockQuantity() < quantity) {
                return CartResponse.error("Không đủ hàng trong kho");
            }
            
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
            
            // Recalculate cart totals
            cart.calculateTotalAmount();
            cartRepository.save(cart);
            
            // Get updated cart count
            Long totalItems = cartRepository.countItemsByUserId(userId);
            
            logger.info("Successfully updated cart item for user {}", userId);
            return CartResponse.success("Đã cập nhật giỏ hàng", CartDTO.fromCart(cart), totalItems);
            
        } catch (Exception e) {
            logger.error("Error updating cart item for user {}: {}", userId, e.getMessage());
            return CartResponse.error("Có lỗi xảy ra khi cập nhật giỏ hàng");
        }
    }
    
    /**
     * Remove product from cart
     */
    public CartResponse removeFromCart(Long userId, Long productId) {
        try {
            logger.info("Removing product {} from cart for user {}", productId, userId);
            
            Cart cart = getOrCreateCart(userId);
            Optional<CartItem> cartItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
            
            if (cartItemOpt.isEmpty()) {
                return CartResponse.error("Sản phẩm không có trong giỏ hàng");
            }
            
            CartItem cartItem = cartItemOpt.get();
            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
            
            // Recalculate cart totals
            cart.calculateTotalAmount();
            cartRepository.save(cart);
            
            // Get updated cart count
            Long totalItems = cartRepository.countItemsByUserId(userId);
            
            logger.info("Successfully removed product {} from cart for user {}", productId, userId);
            return CartResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", CartDTO.fromCart(cart), totalItems);
            
        } catch (Exception e) {
            logger.error("Error removing product {} from cart for user {}: {}", productId, userId, e.getMessage());
            return CartResponse.error("Có lỗi xảy ra khi xóa sản phẩm khỏi giỏ hàng");
        }
    }
    
    /**
     * Get user's cart
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        try {
            Optional<Cart> cartOpt = cartRepository.findByUserIdWithItems(userId);
            
            if (cartOpt.isEmpty()) {
                // Return empty cart DTO directly
                CartDTO emptyCartDTO = new CartDTO();
                emptyCartDTO.setUserId(userId);
                emptyCartDTO.setTotalAmount(java.math.BigDecimal.ZERO);
                emptyCartDTO.setTotalQuantity(0);
                emptyCartDTO.setItems(new java.util.ArrayList<>());
                return CartResponse.success("Giỏ hàng trống", emptyCartDTO, 0L);
            }
            
            Cart cart = cartOpt.get();
            Long totalItems = cartRepository.countItemsByUserId(userId);
            
            return CartResponse.success("Lấy giỏ hàng thành công", CartDTO.fromCart(cart), totalItems);
            
        } catch (Exception e) {
            logger.error("Error getting cart for user {}: {}", userId, e.getMessage());
            return CartResponse.error("Có lỗi xảy ra khi lấy giỏ hàng");
        }
    }
    
    /**
     * Clear user's cart
     */
    public CartResponse clearCart(Long userId) {
        try {
            logger.info("Clearing cart for user {}", userId);
            
            Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
            if (cartOpt.isEmpty()) {
                return CartResponse.error("Giỏ hàng không tồn tại");
            }
            
            Cart cart = cartOpt.get();
            cart.clear();
            cart.calculateTotalAmount();
            cartRepository.save(cart);
            
            logger.info("Successfully cleared cart for user {}", userId);
            return CartResponse.success("Đã xóa tất cả sản phẩm khỏi giỏ hàng", CartDTO.fromCart(cart), 0L);
            
        } catch (Exception e) {
            logger.error("Error clearing cart for user {}: {}", userId, e.getMessage());
            return CartResponse.error("Có lỗi xảy ra khi xóa giỏ hàng");
        }
    }
    
    /**
     * Get user's cart items count
     */
    @Transactional(readOnly = true)
    public Long getCartItemsCount(Long userId) {
        try {
            return cartRepository.countItemsByUserId(userId);
        } catch (Exception e) {
            logger.error("Error getting cart items count for user {}: {}", userId, e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Remove specific products from cart by product IDs
     */
    public CartResponse removeProductsFromCart(Long userId, java.util.List<Long> productIds) {
        try {
            logger.info("Removing {} products from cart for user {}", productIds.size(), userId);
            
            Optional<Cart> cartOpt = cartRepository.findByUserId(userId);
            if (cartOpt.isEmpty()) {
                return CartResponse.error("Giỏ hàng không tồn tại");
            }
            
            Cart cart = cartOpt.get();
            
            // Remove each product from cart
            for (Long productId : productIds) {
                Optional<CartItem> cartItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
                if (cartItemOpt.isPresent()) {
                    CartItem cartItem = cartItemOpt.get();
                    cart.removeItem(cartItem);
                    cartItemRepository.delete(cartItem);
                    logger.info("Removed product {} from cart for user {}", productId, userId);
                }
            }
            
            // Recalculate cart totals
            cart.calculateTotalAmount();
            cartRepository.save(cart);
            
            // Get updated cart count
            Long totalItems = cartRepository.countItemsByUserId(userId);
            
            logger.info("Successfully removed {} products from cart for user {}", productIds.size(), userId);
            return CartResponse.success("Đã xóa sản phẩm đã đặt hàng khỏi giỏ hàng", CartDTO.fromCart(cart), totalItems);
            
        } catch (Exception e) {
            logger.error("Error removing products from cart for user {}: {}", userId, e.getMessage());
            return CartResponse.error("Có lỗi xảy ra khi xóa sản phẩm khỏi giỏ hàng");
        }
    }
}
