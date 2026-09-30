package com.example.demo.repository;

import com.example.demo.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    
    Optional<RefundTransaction> findByRefundId(String refundId);
    
    List<RefundTransaction> findByOrderId(String orderId);
    
    List<RefundTransaction> findByUserId(Long userId);
    
    List<RefundTransaction> findByStatus(RefundTransaction.RefundStatus status);
    
    @Query("SELECT rt FROM RefundTransaction rt WHERE rt.orderId = :orderId AND rt.status = :status")
    List<RefundTransaction> findByOrderIdAndStatus(@Param("orderId") String orderId, 
                                                   @Param("status") RefundTransaction.RefundStatus status);
    
    @Query("SELECT COUNT(rt) FROM RefundTransaction rt WHERE rt.orderId = :orderId AND rt.status = 'SUCCESS'")
    long countSuccessfulRefundsByOrderId(@Param("orderId") String orderId);
    
    @Query("SELECT SUM(rt.amount) FROM RefundTransaction rt WHERE rt.orderId = :orderId AND rt.status = 'SUCCESS'")
    java.math.BigDecimal getTotalRefundedAmountByOrderId(@Param("orderId") String orderId);
}
