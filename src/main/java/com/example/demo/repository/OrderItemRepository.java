package com.example.demo.repository;

import com.example.demo.entity.OrderItem;
import com.example.demo.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for OrderItem entity
 * Following rules.mdc specifications for data access layer
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find completed order items by user
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o JOIN FETCH oi.product p " +
           "WHERE o.user.id = :userId AND o.status = :status")
    List<OrderItem> findCompletedOrderItemsByUser(@Param("userId") Long userId, @Param("status") OrderStatus status);
    
    /**
     * Find completed order items by user and product
     */
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.order o JOIN FETCH oi.product p " +
           "WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status = :status")
    List<OrderItem> findCompletedOrderItemsByUserAndProduct(@Param("userId") Long userId, 
                                                           @Param("productId") Long productId, 
                                                           @Param("status") OrderStatus status);
    
    /**
     * Find order items by order ID
     */
    List<OrderItem> findByOrderId(String orderId);
    
    /**
     * Find order items by product ID
     */
    List<OrderItem> findByProductId(Long productId);
}