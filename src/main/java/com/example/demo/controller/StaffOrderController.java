package com.example.demo.controller;

import com.example.demo.dto.OrderDTO;
import com.example.demo.entity.enums.OrderStatus;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

/**
 * Staff Orders Management Controller (MVC)
 * Mirrors admin orders list/detail for staff portal
 */
@Controller
@RequestMapping("/staff/orders")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
@Slf4j
public class StaffOrderController extends BaseController {

    private final OrderService orderService;

    @GetMapping({"", "/"})
    public String ordersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model,
            Authentication authentication) {
        try {
            Long staffId = getUserIdFromAuthentication(authentication);
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

            model.addAttribute("pageTitle", "Đơn hàng - Staff Portal");
            model.addAttribute("currentPath", "/staff/orders");
            model.addAttribute("contentTemplate", "staff/orders/index");
            model.addAttribute("orders", orders);
            model.addAttribute("staffId", staffId);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", orders.getTotalPages());
            model.addAttribute("totalElements", orders.getTotalElements());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("currentStatus", status);
            model.addAttribute("currentSearch", search);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            model.addAttribute("orderStatuses", Arrays.asList(OrderStatus.values()));

            return "layouts/staff";
        } catch (Exception e) {
            log.error("Error loading staff orders page", e);
            model.addAttribute("error", "Không thể tải danh sách đơn hàng");
            return "layouts/staff";
        }
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable String orderId, Model model, Authentication authentication) {
        try {
            OrderDTO order = orderService.getOrderById(orderId);
            if (order == null) {
                return "redirect:/staff/orders";
            }

            model.addAttribute("pageTitle", "Chi tiết Đơn hàng #" + orderId);
            model.addAttribute("currentPath", "/staff/orders");
            model.addAttribute("contentTemplate", "staff/orders/detail");
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", List.of(OrderStatus.values()));
            model.addAttribute("staffId", getUserIdFromAuthentication(authentication));

            return "layouts/staff";
        } catch (Exception e) {
            log.error("Error loading staff order detail {}", orderId, e);
            return "redirect:/staff/orders";
        }
    }
}


