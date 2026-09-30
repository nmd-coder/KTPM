package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.service.OrderService;
import com.example.demo.util.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Staff Orders REST API
 * Mirrors admin endpoints, restricted to STAFF role
 */
@RestController
@RequestMapping("/api/staff/orders")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Staff Orders API")
public class StaffOrderApiController {

    private final OrderService orderService;

    @Operation(summary = "Danh sách đơn hàng (phân trang, lọc)")
    @GetMapping
    public ResponseEntity<ResponseWrapper<Page<OrderDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<OrderDTO> orders;
            if (fromDate != null && !fromDate.isBlank() && toDate != null && !toDate.isBlank()) {
                java.time.LocalDateTime start = java.time.LocalDate.parse(fromDate).atStartOfDay();
                java.time.LocalDateTime end = java.time.LocalDate.parse(toDate).atTime(23,59,59);
                orders = orderService.getOrdersBetweenDates(start, end, pageable);
            } else if (status != null && !status.isEmpty()) {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderService.getOrdersByStatus(orderStatus, pageable);
            } else if (search != null && !search.trim().isEmpty()) {
                orders = orderService.searchOrders(search.trim(), pageable);
            } else {
                orders = orderService.getAllOrders(pageable);
            }

            return ResponseEntity.ok(ResponseWrapper.success(orders, "Lấy danh sách đơn hàng thành công"));
        } catch (Exception e) {
            log.error("Error listing staff orders", e);
            return ResponseEntity.internalServerError()
                    .body(ResponseWrapper.error("Có lỗi xảy ra khi tải danh sách đơn hàng"));
        }
    }

    @Operation(summary = "Chi tiết đơn hàng")
    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseWrapper<OrderDTO>> detail(@PathVariable String orderId) {
        try {
            OrderDTO order = orderService.getOrderById(orderId);
            if (order == null) {
                return ResponseEntity.status(404).body(ResponseWrapper.error("Không tìm thấy đơn hàng"));
            }
            return ResponseEntity.ok(ResponseWrapper.success(order));
        } catch (Exception e) {
            log.error("Error getting order {}", orderId, e);
            return ResponseEntity.internalServerError()
                    .body(ResponseWrapper.error("Có lỗi xảy ra khi tải chi tiết đơn hàng"));
        }
    }

    @Operation(summary = "Cập nhật trạng thái đơn hàng")
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ResponseWrapper<OrderDTO>> updateStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {
        try {
            String statusStr = body.get("status");
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest().body(ResponseWrapper.error("Trạng thái không được để trống"));
            }

            OrderStatus newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
            OrderDTO updated = orderService.updateOrderStatus(orderId, newStatus);
            return ResponseEntity.ok(ResponseWrapper.success(updated, "Cập nhật trạng thái đơn hàng thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ResponseWrapper.error("Trạng thái không hợp lệ"));
        } catch (Exception e) {
            log.error("Error updating order status {}", orderId, e);
            return ResponseEntity.internalServerError()
                    .body(ResponseWrapper.error("Có lỗi xảy ra khi cập nhật trạng thái đơn hàng"));
        }
    }
}


