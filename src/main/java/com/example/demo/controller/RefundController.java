package com.example.demo.controller;

import com.example.demo.entity.RefundTransaction;
import com.example.demo.service.MoMoRefundService;
import com.example.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "💰 Refund Management", description = "APIs for managing refunds - Admin only")
public class RefundController {
    
    private final MoMoRefundService moMoRefundService;
    private final OrderService orderService;
    
    @Operation(
        summary = "Hoàn tiền thủ công cho đơn hàng",
        description = "Admin có thể hoàn tiền thủ công cho đơn hàng đã thanh toán qua MoMo"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hoàn tiền thành công"),
        @ApiResponse(responseCode = "400", description = "Đơn hàng không hợp lệ hoặc đã được hoàn tiền"),
        @ApiResponse(responseCode = "500", description = "Lỗi server")
    })
    @PostMapping("/manual/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> manualRefund(
            @PathVariable String orderId,
            @RequestParam String reason) {
        
        try {
            log.info("Admin initiating manual refund for order: {}, reason: {}", orderId, reason);
            
            // Kiểm tra đơn hàng có tồn tại không
            var orderDTO = orderService.getOrderById(orderId);
            if (orderDTO == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Đơn hàng không tồn tại"));
            }
            
            // Lấy Order entity từ repository
            var order = orderService.getOrderEntityById(orderId);
            if (order == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Đơn hàng không tồn tại"));
            }
            
            // Kiểm tra đơn hàng đã được hoàn tiền chưa
            if (moMoRefundService.isOrderRefunded(orderId)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Đơn hàng đã được hoàn tiền trước đó"));
            }
            
            // Kiểm tra phương thức thanh toán
            if (order.getPaymentMethod() != com.example.demo.entity.enums.PaymentMethod.MOMO) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Chỉ có thể hoàn tiền cho đơn hàng thanh toán qua MoMo"));
            }
            
            // Thực hiện hoàn tiền
            RefundTransaction refundTransaction = moMoRefundService.processRefund(order, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Hoàn tiền đã được xử lý");
            response.put("refundId", refundTransaction.getRefundId());
            response.put("status", refundTransaction.getStatus());
            response.put("amount", refundTransaction.getAmount());
            
            log.info("Manual refund processed successfully for order: {}, refundId: {}", 
                    orderId, refundTransaction.getRefundId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing manual refund for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Có lỗi xảy ra khi xử lý hoàn tiền: " + e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Lấy danh sách hoàn tiền theo đơn hàng",
        description = "Lấy tất cả giao dịch hoàn tiền của một đơn hàng"
    )
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRefundsByOrder(@PathVariable String orderId) {
        try {
            List<RefundTransaction> refunds = moMoRefundService.getRefundsByOrderId(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("refunds", refunds);
            response.put("totalRefunded", moMoRefundService.getTotalRefundedAmount(orderId));
            response.put("isRefunded", moMoRefundService.isOrderRefunded(orderId));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting refunds for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Có lỗi xảy ra khi lấy thông tin hoàn tiền"));
        }
    }
    
    @Operation(
        summary = "Kiểm tra trạng thái hoàn tiền",
        description = "Kiểm tra xem đơn hàng đã được hoàn tiền chưa"
    )
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRefundStatus(@PathVariable String orderId) {
        try {
            boolean isRefunded = moMoRefundService.isOrderRefunded(orderId);
            var totalRefunded = moMoRefundService.getTotalRefundedAmount(orderId);
            var refunds = moMoRefundService.getRefundsByOrderId(orderId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("isRefunded", isRefunded);
            response.put("totalRefunded", totalRefunded);
            response.put("refundCount", refunds.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking refund status for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Có lỗi xảy ra khi kiểm tra trạng thái hoàn tiền"));
        }
    }
}
