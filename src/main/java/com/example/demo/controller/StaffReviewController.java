package com.example.demo.controller;

import com.example.demo.dto.AdminReviewResponseRequest;
import com.example.demo.dto.OrderReviewGroupDTO;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.dto.ReviewAiAnalysisResponse;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AdminAiInsightsService;
import com.example.demo.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Staff REST API Controller for Review Management
 * Following rules.mdc specifications for REST API
 */
@RestController
@RequestMapping("/staff/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "👨‍💼 Staff Reviews", description = "Staff APIs - Manage product reviews")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
public class StaffReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final AdminAiInsightsService adminAiInsightsService;

    /**
     * Get all reviews with pagination
     */
    @GetMapping
    @Operation(
        summary = "Lấy danh sách đánh giá",
        description = "Lấy danh sách tất cả đánh giá với phân trang (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<ResponseWrapper<Page<ReviewResponse>>> getAllReviews(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số đánh giá mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Lọc theo rating (1-5)", example = "5")
            @RequestParam(required = false) Integer rating,
            @Parameter(description = "Tìm kiếm theo tên sản phẩm hoặc người dùng", example = "hoa hồng")
            @RequestParam(required = false) String search,
            @Parameter(description = "Lọc theo sentiment", example = "POSITIVE")
            @RequestParam(required = false) String sentiment,
            @Parameter(description = "Lọc theo trạng thái", example = "verified")
            @RequestParam(required = false) String status,
            @Parameter(description = "Sắp xếp", example = "newest")
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication) {
        
        try {
            log.info("Admin {} getting all reviews with pagination: page={}, size={}, rating={}, search={}, sentiment={}, status={}, sort={}", 
                    authentication.getName(), page, size, rating, search, sentiment, status, sort);
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Get all reviews with pagination and filters
            Page<Review> reviewsPage = reviewService.getAllReviews(pageable, rating, search, sentiment, status, sort);
            
            // Convert to DTO to avoid lazy loading issues
            Page<ReviewResponse> responsePage = reviewsPage.map(review -> new ReviewResponse(review, false));
            
            return ResponseEntity.ok(ResponseWrapper.success(responsePage));
            
        } catch (Exception e) {
            log.error("Error getting all reviews: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy danh sách đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Get review by ID (Admin only)
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Lấy chi tiết đánh giá",
        description = "Lấy chi tiết đánh giá theo ID (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đánh giá không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewResponse>> getReviewById(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            log.info("Admin {} getting review details for ID: {}", authentication.getName(), id);
            
            Review review = reviewService.getReviewById(id);
            ReviewResponse response = new ReviewResponse(review, false);
            
            return ResponseEntity.ok(ResponseWrapper.success(response));
            
        } catch (Exception e) {
            log.error("Error getting review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy chi tiết đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Delete a review (Admin only)
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Xóa đánh giá",
        description = "Xóa đánh giá bất kỳ (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa đánh giá thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đánh giá không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<String>> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            log.info("Admin {} deleting review {}", authentication.getName(), id);
            
            // Admin can delete any review, so we pass a dummy user ID and isAdmin = true
            reviewService.deleteReview(id, 0L, true);
            
            log.info("Review {} deleted successfully by admin {}", id, authentication.getName());
            return ResponseEntity.ok(ResponseWrapper.success("Đánh giá đã được xóa thành công"));
            
        } catch (Exception e) {
            log.error("Error deleting review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể xóa đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Get review statistics
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Thống kê đánh giá",
        description = "Lấy thống kê tổng quan về đánh giá (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<ResponseWrapper<java.util.Map<String, Object>>> getReviewStatistics(
            Authentication authentication) {
        
        try {
            log.info("Admin {} getting review statistics", authentication.getName());
            
            java.util.Map<String, Object> statistics = reviewService.getReviewStatistics();
            
            return ResponseEntity.ok(ResponseWrapper.success(statistics));
            
        } catch (Exception e) {
            log.error("Error getting review statistics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy thống kê: " + e.getMessage()));
        }
    }

    /**
     * Add admin response to a review
     */
    @PostMapping("/{id}/respond")
    @Operation(
        summary = "Phản hồi đánh giá",
        description = "Admin phản hồi đánh giá của khách hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phản hồi thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đánh giá không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewResponse>> addAdminResponse(
            @PathVariable Long id,
            @Valid @RequestBody AdminReviewResponseRequest request,
            Authentication authentication) {
        
        try {
            log.info("Admin {} adding response to review {}", authentication.getName(), id);
            
            User adminUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            Review review = reviewService.addAdminResponse(id, adminUser.getId(), request.getAdminResponse());
            ReviewResponse response = new ReviewResponse(review, false);
            
            log.info("Admin response added successfully to review {}", id);
            return ResponseEntity.ok(ResponseWrapper.success(response, "Phản hồi đã được thêm thành công"));
            
        } catch (Exception e) {
            log.error("Error adding admin response to review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể thêm phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Update admin response for a review
     */
    @PutMapping("/{id}/response")
    @Operation(
        summary = "Cập nhật phản hồi đánh giá",
        description = "Admin cập nhật phản hồi đánh giá của khách hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật phản hồi thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đánh giá không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewResponse>> updateAdminResponse(
            @PathVariable Long id,
            @Valid @RequestBody AdminReviewResponseRequest request,
            Authentication authentication) {
        
        try {
            log.info("Admin {} updating response for review {}", authentication.getName(), id);
            
            User adminUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            Review review = reviewService.updateAdminResponse(id, adminUser.getId(), request.getAdminResponse());
            ReviewResponse response = new ReviewResponse(review, false);
            
            log.info("Admin response updated successfully for review {}", id);
            return ResponseEntity.ok(ResponseWrapper.success(response, "Phản hồi đã được cập nhật thành công"));
            
        } catch (Exception e) {
            log.error("Error updating admin response for review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể cập nhật phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Remove admin response from a review
     */
    @DeleteMapping("/{id}/response")
    @Operation(
        summary = "Xóa phản hồi đánh giá",
        description = "Admin xóa phản hồi đánh giá của khách hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa phản hồi thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đánh giá không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewResponse>> removeAdminResponse(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            log.info("Admin {} removing response from review {}", authentication.getName(), id);
            
            User adminUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            Review review = reviewService.removeAdminResponse(id, adminUser.getId());
            ReviewResponse response = new ReviewResponse(review, false);
            
            log.info("Admin response removed successfully from review {}", id);
            return ResponseEntity.ok(ResponseWrapper.success(response, "Phản hồi đã được xóa thành công"));
            
        } catch (Exception e) {
            log.error("Error removing admin response from review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể xóa phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Analyze review with AI to get sentiment and suggested replies
     */
    @PostMapping("/{id}/analyze")
    @Operation(
        summary = "Phân tích đánh giá với AI",
        description = "Sử dụng AI để phân tích sentiment và gợi ý câu trả lời cho đánh giá"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phân tích thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đánh giá không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewAiAnalysisResponse>> analyzeReview(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            log.info("Admin {} analyzing review {} with AI", authentication.getName(), id);
            
            ReviewAiAnalysisResponse analysis = adminAiInsightsService.analyzeReview(id);
            
            log.info("AI analysis completed for review {}", id);
            return ResponseEntity.ok(ResponseWrapper.success(analysis, "Phân tích AI hoàn thành"));
            
        } catch (Exception e) {
            log.error("Error analyzing review {} with AI: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể phân tích đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Export reviews to Excel
     */
    @GetMapping("/export")
    @Operation(
        summary = "Xuất danh sách đánh giá ra Excel",
        description = "Xuất tất cả đánh giá ra file Excel (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xuất file thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<byte[]> exportReviews(
            @Parameter(description = "Lọc theo rating (1-5)", example = "5")
            @RequestParam(required = false) Integer rating,
            @Parameter(description = "Tìm kiếm theo tên sản phẩm hoặc người dùng", example = "hoa hồng")
            @RequestParam(required = false) String search,
            @Parameter(description = "Lọc theo sentiment", example = "POSITIVE")
            @RequestParam(required = false) String sentiment,
            @Parameter(description = "Lọc theo trạng thái", example = "verified")
            @RequestParam(required = false) String status,
            @Parameter(description = "Sắp xếp", example = "newest")
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication) {
        
        try {
            log.info("Admin {} exporting reviews to Excel with filters: rating={}, search={}, sentiment={}, status={}, sort={}", 
                    authentication.getName(), rating, search, sentiment, status, sort);
            
            // Get all reviews without pagination for export
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<Review> reviewsPage = reviewService.getAllReviews(pageable, rating, search, sentiment, status, sort);
            List<Review> reviews = reviewsPage.getContent();
            
            // Create Excel workbook
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Đánh giá");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setWrapText(true);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Sản phẩm", "Khách hàng", "Đánh giá", "Bình luận", "Sentiment", "Phản hồi Admin", "Ngày tạo"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fill data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (Review review : reviews) {
                Row row = sheet.createRow(rowNum++);
                
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(review.getId());
                cell0.setCellStyle(dataStyle);
                
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(review.getProduct() != null ? review.getProduct().getName() : "N/A");
                cell1.setCellStyle(dataStyle);
                
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(review.getUser() != null ? 
                    review.getUser().getFirstname() + " " + review.getUser().getLastname() : "N/A");
                cell2.setCellStyle(dataStyle);
                
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(review.getRating() + " ⭐");
                cell3.setCellStyle(dataStyle);
                
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(review.getComment() != null ? review.getComment() : "");
                cell4.setCellStyle(dataStyle);
                
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(review.getSentiment() != null ? review.getSentiment() : "");
                cell5.setCellStyle(dataStyle);
                
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(review.getAdminResponse() != null ? review.getAdminResponse() : "");
                cell6.setCellStyle(dataStyle);
                
                Cell cell7 = row.createCell(7);
                cell7.setCellValue(review.getCreatedAt() != null ? review.getCreatedAt().format(formatter) : "");
                cell7.setCellStyle(dataStyle);
            }
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                // Set max width to avoid too wide columns
                if (sheet.getColumnWidth(i) > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            byte[] excelBytes = outputStream.toByteArray();
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "danh-gia-" + System.currentTimeMillis() + ".xlsx");
            headers.setContentLength(excelBytes.length);
            
            log.info("Excel file exported successfully with {} reviews", reviews.size());
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
            
        } catch (Exception e) {
            log.error("Error exporting reviews to Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get reviews grouped by order with pagination (Staff)
     */
    @GetMapping("/by-orders")
    @Operation(
        summary = "Lấy đánh giá nhóm theo đơn hàng",
        description = "Lấy danh sách đánh giá được nhóm theo đơn hàng với phân trang (Staff)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ResponseEntity<ResponseWrapper<Page<OrderReviewGroupDTO>>> getReviewsGroupedByOrder(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số đánh giá mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Tìm kiếm theo tên khách hàng hoặc mã đơn hàng", example = "Nguyen Van A")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sắp xếp", example = "newest")
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication) {
        
        try {
            log.info("Staff {} getting reviews grouped by order: page={}, size={}, search={}, sort={}", 
                    authentication.getName(), page, size, search, sort);
            
            Pageable pageable = PageRequest.of(page, size);
            
            // Get reviews grouped by order
            Page<OrderReviewGroupDTO> orderReviewsPage = reviewService.getReviewsGroupedByOrder(pageable, search, sort);
            
            return ResponseEntity.ok(ResponseWrapper.success(orderReviewsPage));
            
        } catch (Exception e) {
            log.error("Error getting reviews grouped by order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy danh sách đánh giá theo đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Get order review group by order ID (Staff)
     */
    @GetMapping("/orders/{orderId}")
    @Operation(
        summary = "Lấy chi tiết đánh giá theo đơn hàng",
        description = "Lấy chi tiết tất cả đánh giá trong một đơn hàng (Staff)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại hoặc chưa có đánh giá")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> getOrderReviewGroup(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            log.info("Staff {} getting order review group for order: {}", authentication.getName(), orderId);
            
            java.util.Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error getting order review group for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể lấy chi tiết đánh giá đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Add bulk admin response to all reviews in an order (Staff)
     */
    @PostMapping("/orders/{orderId}/respond")
    @Operation(
        summary = "Phản hồi tất cả đánh giá trong đơn hàng",
        description = "Staff phản hồi cho tất cả đánh giá trong một đơn hàng cùng lúc"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phản hồi thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> addBulkAdminResponse(
            @PathVariable String orderId,
            @Valid @RequestBody AdminReviewResponseRequest request,
            Authentication authentication) {
        
        try {
            log.info("Staff {} adding bulk response to order {}", authentication.getName(), orderId);
            
            User staffUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Staff user not found"));
            
            // Add bulk admin response
            java.util.List<Review> updatedReviews = reviewService.addBulkAdminResponse(orderId, staffUser.getId(), request.getAdminResponse());
            
            // Get updated order review group
            java.util.Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                log.info("Bulk admin response added successfully to {} reviews in order {}", updatedReviews.size(), orderId);
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get(), 
                    String.format("Phản hồi đã được thêm cho %d đánh giá trong đơn hàng", updatedReviews.size())));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Không thể lấy thông tin đơn hàng sau khi cập nhật"));
            }
            
        } catch (Exception e) {
            log.error("Error adding bulk admin response to order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể thêm phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Update bulk admin response for all reviews in an order (Staff)
     */
    @PutMapping("/orders/{orderId}/response")
    @Operation(
        summary = "Cập nhật phản hồi tất cả đánh giá trong đơn hàng",
        description = "Staff cập nhật phản hồi cho tất cả đánh giá trong một đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật phản hồi thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> updateBulkAdminResponse(
            @PathVariable String orderId,
            @Valid @RequestBody AdminReviewResponseRequest request,
            Authentication authentication) {
        
        try {
            log.info("Staff {} updating bulk response for order {}", authentication.getName(), orderId);
            
            User staffUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Staff user not found"));
            
            // Update bulk admin response
            java.util.List<Review> updatedReviews = reviewService.updateBulkAdminResponse(orderId, staffUser.getId(), request.getAdminResponse());
            
            // Get updated order review group
            java.util.Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                log.info("Bulk admin response updated successfully for {} reviews in order {}", updatedReviews.size(), orderId);
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get(), 
                    String.format("Phản hồi đã được cập nhật cho %d đánh giá trong đơn hàng", updatedReviews.size())));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Không thể lấy thông tin đơn hàng sau khi cập nhật"));
            }
            
        } catch (Exception e) {
            log.error("Error updating bulk admin response for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể cập nhật phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Remove bulk admin response from all reviews in an order (Staff)
     */
    @DeleteMapping("/orders/{orderId}/response")
    @Operation(
        summary = "Xóa phản hồi tất cả đánh giá trong đơn hàng",
        description = "Staff xóa phản hồi cho tất cả đánh giá trong một đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xóa phản hồi thành công"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<OrderReviewGroupDTO>> removeBulkAdminResponse(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            log.info("Staff {} removing bulk response from order {}", authentication.getName(), orderId);
            
            User staffUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Staff user not found"));
            
            // Remove bulk admin response
            java.util.List<Review> updatedReviews = reviewService.removeBulkAdminResponse(orderId, staffUser.getId());
            
            // Get updated order review group
            java.util.Optional<OrderReviewGroupDTO> orderReviewGroup = reviewService.getOrderReviewGroup(orderId);
            
            if (orderReviewGroup.isPresent()) {
                log.info("Bulk admin response removed successfully from {} reviews in order {}", updatedReviews.size(), orderId);
                return ResponseEntity.ok(ResponseWrapper.success(orderReviewGroup.get(), 
                    String.format("Phản hồi đã được xóa khỏi %d đánh giá trong đơn hàng", updatedReviews.size())));
            } else {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Không thể lấy thông tin đơn hàng sau khi cập nhật"));
            }
            
        } catch (Exception e) {
            log.error("Error removing bulk admin response from order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Không thể xóa phản hồi: " + e.getMessage()));
        }
    }

    /**
     * Analyze order reviews with AI to get sentiment and suggested replies (Staff)
     */
    @PostMapping("/orders/{orderId}/analyze")
    @Operation(
        summary = "Phân tích đánh giá đơn hàng với AI",
        description = "Sử dụng AI để phân tích sentiment và gợi ý câu trả lời cho tất cả đánh giá trong đơn hàng"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phân tích AI thành công"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
        @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
        @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
    })
    public ResponseEntity<ResponseWrapper<ReviewAiAnalysisResponse>> analyzeOrderReviews(
            @PathVariable String orderId,
            Authentication authentication) {
        
        try {
            log.info("Staff {} analyzing order reviews for order {} with AI", authentication.getName(), orderId);
            
            ReviewAiAnalysisResponse analysis = adminAiInsightsService.analyzeOrderReviews(orderId);
            
            log.info("AI analysis completed for order {}", orderId);
            return ResponseEntity.ok(ResponseWrapper.success(analysis, "Phân tích AI hoàn thành"));
        } catch (IllegalArgumentException e) {
            log.warn("Order not found: {}", orderId);
            return ResponseEntity.status(404)
                    .body(ResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error analyzing order reviews for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ResponseWrapper.error("Có lỗi xảy ra khi phân tích đánh giá: " + e.getMessage()));
        }
    }
}
