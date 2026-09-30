package com.example.demo.service;

import com.example.demo.config.MoMoProperties;
import com.example.demo.entity.Order;
import com.example.demo.entity.RefundTransaction;
import com.example.demo.entity.enums.PaymentMethod;
import com.example.demo.repository.RefundTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoRefundService {
    
    private final MoMoProperties momoProperties;
    private final RefundTransactionRepository refundTransactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Thực hiện hoàn tiền MoMo cho đơn hàng
     */
    @Transactional
    public RefundTransaction processRefund(Order order, String reason) {
        log.info("Processing MoMo refund for order: {}", order.getId());
        
        try {
            // Kiểm tra xem đã có hoàn tiền thành công chưa
            long successfulRefunds = refundTransactionRepository.countSuccessfulRefundsByOrderId(order.getId());
            if (successfulRefunds > 0) {
                log.warn("Order {} already has successful refunds", order.getId());
                throw new RuntimeException("Đơn hàng đã được hoàn tiền thành công trước đó");
            }
            
            // Tạo refund transaction
            String refundId = "REFUND-" + order.getId() + "-" + System.currentTimeMillis();
            RefundTransaction refundTransaction = RefundTransaction.builder()
                    .refundId(refundId)
                    .orderId(order.getId())
                    .momoTransId(extractMomoTransId(order))
                    .amount(order.getTotalAmount())
                    .status(RefundTransaction.RefundStatus.PENDING)
                    .paymentMethod(PaymentMethod.MOMO)
                    .reason(reason)
                    .user(order.getUser())
                    .createdAt(LocalDateTime.now())
                    .build();
            
            refundTransaction = refundTransactionRepository.save(refundTransaction);
            
            // Gọi MoMo API để hoàn tiền
            Map<String, Object> refundResult = callMoMoRefundAPI(refundTransaction);
            
            // Cập nhật kết quả hoàn tiền
            updateRefundResult(refundTransaction, refundResult);
            
            log.info("MoMo refund processed for order: {}, refundId: {}, status: {}", 
                    order.getId(), refundId, refundTransaction.getStatus());
            
            return refundTransaction;
            
        } catch (Exception e) {
            log.error("Error processing MoMo refund for order {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Có lỗi xảy ra khi xử lý hoàn tiền: " + e.getMessage());
        }
    }
    
    /**
     * Gọi MoMo Refund API
     */
    private Map<String, Object> callMoMoRefundAPI(RefundTransaction refundTransaction) {
        try {
            String partnerCode = momoProperties.getPartnerCode();
            String accessKey = momoProperties.getAccessKey();
            String secretKey = momoProperties.getSecretKey();
            String requestId = "REQ-REFUND-" + System.currentTimeMillis();
            String orderId = refundTransaction.getOrderId();
            String transId = refundTransaction.getMomoTransId();
            String amount = refundTransaction.getAmount().toBigInteger().toString();
            String description = "Hoan tien don hang #" + orderId + " - " + refundTransaction.getReason();
            
            // Tạo raw signature cho refund theo format MoMo (thứ tự quan trọng)
            // requestType KHÔNG được bao gồm trong raw signature
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&description=" + description +
                    "&orderId=" + orderId +
                    "&partnerCode=" + partnerCode +
                    "&requestId=" + requestId +
                    "&transId=" + transId;
            
            String signature = hmacSHA256(rawSignature, secretKey);
            
            log.info("MoMo Refund Request - Raw signature: {}", rawSignature);
            log.info("MoMo Refund Request - Signature: {}", signature);
            
            // Tạo payload theo format MoMo Refund API
            Map<String, Object> payload = new HashMap<>();
            payload.put("partnerCode", partnerCode);
            payload.put("accessKey", accessKey);
            payload.put("requestId", requestId);
            payload.put("amount", amount);
            payload.put("orderId", orderId);
            payload.put("transId", transId);
            payload.put("description", description);
            payload.put("signature", signature);
            
            // Thêm các trường bắt buộc khác
            payload.put("requestType", "refund");
            
            log.info("MoMo Refund Request - Payload: {}", payload);
            
            String url = momoProperties.getBaseUrl() + momoProperties.getEndpointRefund();
            
            // Gửi request đến MoMo
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String json = objectMapper.writeValueAsString(payload);
            
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(httpRequest, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            log.info("MoMo refund API response: {}", response.body());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error calling MoMo refund API: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi gọi API hoàn tiền MoMo: " + e.getMessage());
        }
    }
    
    /**
     * Cập nhật kết quả hoàn tiền
     */
    private void updateRefundResult(RefundTransaction refundTransaction, Map<String, Object> result) {
        try {
            String resultCode = String.valueOf(result.get("resultCode"));
            String message = String.valueOf(result.get("message"));
            
            refundTransaction.setMomoResultCode(resultCode);
            refundTransaction.setMomoMessage(message);
            refundTransaction.setMomoResponse(objectMapper.writeValueAsString(result));
            refundTransaction.setProcessedAt(LocalDateTime.now());
            
            if ("0".equals(resultCode)) {
                refundTransaction.setStatus(RefundTransaction.RefundStatus.SUCCESS);
                log.info("Refund successful for order: {}", refundTransaction.getOrderId());
            } else {
                refundTransaction.setStatus(RefundTransaction.RefundStatus.FAILED);
                log.warn("Refund failed for order: {}, reason: {}", 
                        refundTransaction.getOrderId(), message);
            }
            
            refundTransactionRepository.save(refundTransaction);
            
        } catch (Exception e) {
            log.error("Error updating refund result: {}", e.getMessage(), e);
            refundTransaction.setStatus(RefundTransaction.RefundStatus.FAILED);
            refundTransaction.setMomoMessage("Lỗi khi cập nhật kết quả hoàn tiền: " + e.getMessage());
            refundTransactionRepository.save(refundTransaction);
        }
    }
    
    /**
     * Trích xuất MoMo transaction ID từ order
     */
    private String extractMomoTransId(Order order) {
        // Lấy momoTransId thực tế đã được lưu khi thanh toán
        String momoTransId = order.getMomoTransId(); 
        
        if (momoTransId == null || momoTransId.isBlank()) {
            log.error("Không thể hoàn tiền: Đơn hàng {} không có MoMo transId.", order.getId());
            throw new RuntimeException("Đơn hàng không có mã giao dịch MoMo để hoàn tiền.");
        }
        
        log.info("Using MoMo transId for refund: {} for order: {}", momoTransId, order.getId());
        return momoTransId;
    }
    
    /**
     * HMAC SHA256 signature
     */
    private String hmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmac);
        } catch (Exception e) {
            log.error("Error creating HMAC signature: {}", e.getMessage());
            throw new RuntimeException("Lỗi tạo chữ ký HMAC");
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Lấy danh sách hoàn tiền theo order
     */
    public java.util.List<RefundTransaction> getRefundsByOrderId(String orderId) {
        return refundTransactionRepository.findByOrderId(orderId);
    }
    
    /**
     * Kiểm tra xem đơn hàng đã được hoàn tiền thành công chưa
     */
    public boolean isOrderRefunded(String orderId) {
        return refundTransactionRepository.countSuccessfulRefundsByOrderId(orderId) > 0;
    }
    
    /**
     * Lấy tổng số tiền đã hoàn cho đơn hàng
     */
    public BigDecimal getTotalRefundedAmount(String orderId) {
        BigDecimal total = refundTransactionRepository.getTotalRefundedAmountByOrderId(orderId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
