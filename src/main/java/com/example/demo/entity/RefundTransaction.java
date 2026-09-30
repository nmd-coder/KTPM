package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.example.demo.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "refund_id", unique = true, nullable = false)
    private String refundId;
    
    @Column(name = "order_id", nullable = false)
    private String orderId;
    
    @Column(name = "momo_trans_id")
    private String momoTransId;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "momo_response", columnDefinition = "TEXT")
    private String momoResponse;
    
    @Column(name = "momo_result_code")
    private String momoResultCode;
    
    @Column(name = "momo_message")
    private String momoMessage;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    public enum RefundStatus {
        PENDING,    // Đang chờ xử lý
        PROCESSING, // Đang xử lý
        SUCCESS,    // Hoàn tiền thành công
        FAILED,     // Hoàn tiền thất bại
        CANCELLED   // Đã hủy
    }
}
