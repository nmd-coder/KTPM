package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.config.MoMoProperties;
import com.example.demo.entity.enums.PaymentMethod;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Payment Service for handling payment processing
 * Following rules.mdc specifications for business logic tier
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final OrderRepository orderRepository;
    private final MoMoProperties momoProperties;

    /**
     * Process payment for an order
     * @param order Order to process payment for
     * @param paymentMethod Payment method to use
     * @return Payment result with status and message
     */
    public PaymentResult processPayment(Order order, PaymentMethod paymentMethod) {
        try {
            // Validate order
            if (order == null) {
                return PaymentResult.error("Đơn hàng không hợp lệ");
            }
            
            log.info("Processing payment for order {} with method {}", order.getId(), paymentMethod);
            
            if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return PaymentResult.error("Số tiền thanh toán không hợp lệ");
            }
            
            // Check if payment method is available
            if (!paymentMethod.isAvailable()) {
                return PaymentResult.error("Phương thức thanh toán " + paymentMethod.getDisplayName() + " " + paymentMethod.getStatusMessage());
            }
            
            // Process payment based on method
            switch (paymentMethod) {
                case COD:
                    return processCODPayment(order);
                case MOMO:
                    return processMoMoPayment(order);
                case BANK_TRANSFER:
                    return PaymentResult.error("Chuyển khoản ngân hàng chưa được hỗ trợ");
                case CREDIT_CARD:
                    return PaymentResult.error("Thẻ tín dụng chưa được hỗ trợ");
                default:
                    return PaymentResult.error("Phương thức thanh toán không được hỗ trợ");
            }
            
        } catch (Exception e) {
            String orderId = (order != null) ? order.getId().toString() : "unknown";
            log.error("Error processing payment for order {}: {}", orderId, e.getMessage(), e);
            return PaymentResult.error("Có lỗi xảy ra khi xử lý thanh toán");
        }
    }
    
    /**
     * Process Cash on Delivery payment
     * @param order Order to process
     * @return Payment result
     */
    private PaymentResult processCODPayment(Order order) {
        log.info("Processing COD payment for order {}", order.getId());
        
        try {
            // For COD, we just need to confirm the order
            // Payment will be collected when delivering
            order.setStatus(OrderStatus.PROCESSING);
            order.setPaymentMethod(PaymentMethod.COD);
            
            // Save order to database (updatedAt will be automatically set by @LastModifiedDate)
            Order savedOrder = orderRepository.save(order);
            log.info("Order {} status updated to PROCESSING and saved to database", savedOrder.getId());
            
            // Create payment record (if needed)
            Map<String, Object> paymentDetails = new HashMap<>();
            paymentDetails.put("method", "COD");
            paymentDetails.put("status", "PENDING");
            paymentDetails.put("amount", order.getTotalAmount());
            paymentDetails.put("processedAt", LocalDateTime.now());
            paymentDetails.put("note", "Thanh toán khi nhận hàng");
            paymentDetails.put("orderId", savedOrder.getId());
            paymentDetails.put("orderStatus", savedOrder.getStatus().name());
            
            log.info("COD payment processed successfully for order {}", order.getId());
            
            return PaymentResult.success(
                "Đơn hàng đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.",
                paymentDetails
            );
            
        } catch (Exception e) {
            String orderId = (order != null) ? order.getId().toString() : "unknown";
            log.error("Error processing COD payment for order {}: {}", orderId, e.getMessage(), e);
            return PaymentResult.error("Có lỗi xảy ra khi xử lý thanh toán COD");
        }
    }
    
    /**
     * Process MoMo payment (placeholder - under development)
     * @param order Order to process
     * @return Payment result
     */
    private PaymentResult processMoMoPayment(Order order) {
        log.info("Processing MoMo payment for order {}", order.getId());
        try {
            String partnerCode = momoProperties.getPartnerCode();
            String accessKey = momoProperties.getAccessKey();
            String secretKey = momoProperties.getSecretKey();
            String requestId = "REQ-" + System.currentTimeMillis();
            String orderId = "ORDER-" + order.getId() + "-" + System.currentTimeMillis();
            String orderInfo = "Thanh toan don hang #" + order.getId();
            // Use configured URLs from properties
            String redirectUrl = momoProperties.getReturnUrl();
            String ipnUrl = momoProperties.getNotifyUrl();
            
            log.info("Using MoMo callback URLs - Return: {}, Notify: {}", redirectUrl, ipnUrl);
            String amount = order.getTotalAmount().toBigInteger().toString();
            String requestType = momoProperties.getRequestType();
            String extraData = ""; // base64 optional

            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + ipnUrl +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + redirectUrl +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            String signature = hmacSHA256(rawSignature, secretKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("partnerCode", partnerCode);
            payload.put("accessKey", accessKey);
            payload.put("requestId", requestId);
            payload.put("amount", amount);
            payload.put("orderId", orderId);
            payload.put("orderInfo", orderInfo);
            payload.put("redirectUrl", redirectUrl);
            payload.put("ipnUrl", ipnUrl);
            payload.put("extraData", extraData);
            payload.put("requestType", requestType);
            payload.put("signature", signature);

            String url = momoProperties.getBaseUrl() + momoProperties.getEndpointCreate();

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
            log.info("MoMo create response: {}", response.body());

            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body(), Map.class);
            Object payUrl = respMap.get("payUrl");
            Object deeplink = respMap.get("deeplink");
            Object resultCode = respMap.get("resultCode");

            Map<String, Object> details = new HashMap<>();
            details.put("method", "MOMO");
            details.put("amount", order.getTotalAmount());
            details.put("orderId", order.getId());
            details.put("momoOrderId", orderId);
            details.put("payUrl", payUrl);
            details.put("deeplink", deeplink);
            details.put("resultCode", resultCode);

            if ("0".equals(String.valueOf(resultCode)) && payUrl != null) {
                // Mark order as pending payment via MoMo
                order.setPaymentMethod(PaymentMethod.MOMO);
                order.setStatus(OrderStatus.PENDING);
                orderRepository.save(order);
                return PaymentResult.success("Tạo yêu cầu thanh toán MoMo thành công", details);
            }
            return PaymentResult.error("Khởi tạo thanh toán MoMo thất bại", details);
        } catch (Exception e) {
            log.error("MoMo processing error: {}", e.getMessage(), e);
            return PaymentResult.error("Có lỗi khi kết nối MoMo");
        }
    }

    private String hmacSHA256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    
    /**
     * Get available payment methods
     * @return Map of available payment methods
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAvailablePaymentMethods() {
        Map<String, Object> result = new HashMap<>();
        
        for (PaymentMethod method : PaymentMethod.values()) {
            Map<String, Object> methodInfo = new HashMap<>();
            methodInfo.put("displayName", method.getDisplayName());
            methodInfo.put("englishName", method.getEnglishName());
            methodInfo.put("available", method.isAvailable());
            methodInfo.put("statusMessage", method.getStatusMessage());
            
            result.put(method.name(), methodInfo);
        }
        
        return result;
    }
    
    /**
     * Payment result class
     */
    public static class PaymentResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> paymentDetails;
        
        private PaymentResult(boolean success, String message, Map<String, Object> paymentDetails) {
            this.success = success;
            this.message = message;
            this.paymentDetails = paymentDetails;
        }
        
        public static PaymentResult success(String message, Map<String, Object> paymentDetails) {
            return new PaymentResult(true, message, paymentDetails);
        }
        
        public static PaymentResult success(String message) {
            return new PaymentResult(true, message, new HashMap<>());
        }
        
        public static PaymentResult error(String message) {
            return new PaymentResult(false, message, new HashMap<>());
        }
        
        public static PaymentResult error(String message, Map<String, Object> paymentDetails) {
            return new PaymentResult(false, message, paymentDetails);
        }
        
        // Getters
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Map<String, Object> getPaymentDetails() {
            return paymentDetails;
        }
    }
}
