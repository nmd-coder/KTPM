package com.example.demo.controller;

import com.example.demo.dto.AdminProductDTO;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.entity.Catalog;
import com.example.demo.entity.Product;
import com.example.demo.entity.enums.ProductStatus;
import com.example.demo.repository.CatalogRepository;
import com.example.demo.service.ProductService;
import com.example.demo.service.AdminProductAiService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "🌸 Admin Products", description = "Admin product management APIs - CRUD, stock, status, AI description")
@Controller
@RequestMapping("/admin/products")
public class AdminProductController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    @Autowired
    private ProductService productService;
    
    @Autowired
    private CatalogRepository catalogRepository;
    
    @Autowired
    private AdminProductAiService adminProductAiService;
    @Autowired
    private com.example.demo.service.ExcelExportService excelExportService;

    /**
     * Admin Products Management Page
     */
    @GetMapping({"", "/"})
    public String productsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        
        // Create sort object
        Sort sort = sortDir.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get products with filters
        Page<Product> productsPage;
        if (search != null && !search.trim().isEmpty()) {
            productsPage = productService.searchProducts(search.trim(), pageable);
        } else if (status != null && !status.isEmpty()) {
            try {
                ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
                productsPage = productService.getProductsByStatus(productStatus, pageable);
            } catch (IllegalArgumentException e) {
                productsPage = productService.getAllProducts(pageable);
            }
        } else {
            productsPage = productService.getAllProducts(pageable);
        }
        
        // Get statistics for dashboard cards
        Map<String, Object> statistics = productService.getProductStatistics();
        
        // Add attributes to model
        model.addAttribute("productsPage", productsPage);
        model.addAttribute("statistics", statistics);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("search", search);
        model.addAttribute("productStatuses", ProductStatus.values());
        
        // Breadcrumbs
        model.addAttribute("pageTitle", "Quản lý Sản phẩm");
        List<BreadcrumbItem> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbItem("Dashboard", "/admin/dashboard"));
        breadcrumbs.add(new BreadcrumbItem("Quản lý Sản phẩm", "/admin/products"));
        model.addAttribute("breadcrumbs", breadcrumbs);
        
        // Set page title and content template for admin layout
        model.addAttribute("pageTitle", "Quản lý Sản phẩm");
        model.addAttribute("contentTemplate", "admin/products/index");
        model.addAttribute("currentPath", "/admin/products");
        
        return "layouts/admin";
    }

    /**
     * Export products to Excel
     */
    @GetMapping("/api/export")
    @ResponseBody
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        try {
            // Load data without pagination
            java.util.List<com.example.demo.entity.Product> list;
            if (search != null && !search.isBlank()) {
                var page = productService.searchProducts(search.trim(), org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
                list = page.getContent();
            } else if (status != null && !status.isBlank()) {
                var st = com.example.demo.entity.enums.ProductStatus.valueOf(status.toUpperCase());
                var page = productService.getProductsByStatus(st, org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE));
                list = page.getContent();
            } else {
                list = productService.findAll(org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            }

            byte[] bytes = excelExportService.exportProducts(list);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.xlsx");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (Exception e) {
            log.error("Export products failed", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }

    // Product Detail Page removed - now using modal dialog instead

    // ==================== REST API ENDPOINTS ====================

    /**
     * Get products list for AJAX pagination
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Page<Product>>> getProductsList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Product> productsPage;
            if (search != null && !search.trim().isEmpty()) {
                productsPage = productService.searchProducts(search.trim(), pageable);
            } else if (status != null && !status.isEmpty()) {
                try {
                    ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
                    productsPage = productService.getProductsByStatus(productStatus, pageable);
                } catch (IllegalArgumentException e) {
                    productsPage = productService.getAllProducts(pageable);
                }
            } else {
                productsPage = productService.getAllProducts(pageable);
            }
            
            return ResponseEntity.ok(ResponseWrapper.success(productsPage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi tải danh sách sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * Get product statistics
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Map<String, Object>>> getProductStatistics() {
        try {
            Map<String, Object> statistics = productService.getProductStatistics();
            return ResponseEntity.ok(ResponseWrapper.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi tải thống kê: " + e.getMessage()));
        }
    }

    /**
     * Create new product
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<AdminProductDTO>> createProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "0") Integer stockQuantity,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(defaultValue = "500") Integer weightG,
            @RequestParam(defaultValue = "20") Integer lengthCm,
            @RequestParam(defaultValue = "20") Integer widthCm,
            @RequestParam(defaultValue = "30") Integer heightCm,
            @RequestParam(required = false) Long catalogId) {
        
        try {
            // Validate input
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Tên sản phẩm không được để trống"));
            }
            
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Giá sản phẩm không hợp lệ"));
            }
            
            // Create product
            Product product = productService.createProduct(
                name.trim(), description, price, stockQuantity, 
                ProductStatus.valueOf(status.toUpperCase()),
                image, weightG, lengthCm, widthCm, heightCm, catalogId
            );
            
            AdminProductDTO dto = AdminProductDTO.fromEntity(product);
            return ResponseEntity.ok(ResponseWrapper.success(dto, "Tạo sản phẩm thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Dữ liệu không hợp lệ: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi tạo sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * Update product
     */
    @PutMapping("/api/{productId}")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<AdminProductDTO>> updateProduct(
            @PathVariable Long productId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam Integer stockQuantity,
            @RequestParam String status,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam Integer weightG,
            @RequestParam Integer lengthCm,
            @RequestParam Integer widthCm,
            @RequestParam Integer heightCm,
            @RequestParam(required = false) Long catalogId) {
        
        try {
            // Validate input
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Tên sản phẩm không được để trống"));
            }
            
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Giá sản phẩm không hợp lệ"));
            }
            
            // Update product
            Product product = productService.updateProduct(
                productId, name.trim(), description, price, stockQuantity,
                ProductStatus.valueOf(status.toUpperCase()),
                image, weightG, lengthCm, widthCm, heightCm, catalogId
            );
            
            AdminProductDTO dto = AdminProductDTO.fromEntity(product);
            return ResponseEntity.ok(ResponseWrapper.success(dto, "Cập nhật sản phẩm thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Dữ liệu không hợp lệ: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi cập nhật sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * Update product status
     */
    @PutMapping("/api/{productId}/status")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<AdminProductDTO>> updateProductStatus(
            @PathVariable Long productId,
            @RequestParam String status) {
        
        try {
            ProductStatus productStatus = ProductStatus.valueOf(status.toUpperCase());
            Product product = productService.updateProductStatus(productId, productStatus);
            
            AdminProductDTO dto = AdminProductDTO.fromEntity(product);
            return ResponseEntity.ok(ResponseWrapper.success(dto, "Cập nhật trạng thái thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Trạng thái không hợp lệ"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi cập nhật trạng thái: " + e.getMessage()));
        }
    }

    /**
     * Update product stock
     */
    @PutMapping("/api/{productId}/stock")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<AdminProductDTO>> updateProductStock(
            @PathVariable Long productId,
            @RequestParam Integer stockQuantity) {
        
        try {
            if (stockQuantity < 0) {
                return ResponseEntity.badRequest()
                    .body(ResponseWrapper.error("Số lượng tồn kho không được âm"));
            }
            
            Product product = productService.updateProductStock(productId, stockQuantity);
            
            AdminProductDTO dto = AdminProductDTO.fromEntity(product);
            return ResponseEntity.ok(ResponseWrapper.success(dto, "Cập nhật tồn kho thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi cập nhật tồn kho: " + e.getMessage()));
        }
    }

    /**
     * Delete product
     */
    @DeleteMapping("/api/{productId}")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<Void>> deleteProduct(@PathVariable Long productId) {
        try {
            productService.deleteProduct(productId);
            return ResponseEntity.ok(ResponseWrapper.success(null, "Sản phẩm đã được xóa thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi xóa sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * Get product by ID
     */
    @GetMapping("/api/{productId}")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<AdminProductDTO>> getProduct(@PathVariable Long productId) {
        try {
            Map<String, Object> result = productService.getProductByIdWithRating(productId);
            
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseWrapper.error("Không tìm thấy sản phẩm"));
            }
            
            Product product = (Product) result.get("product");
            Double averageRating = (Double) result.get("averageRating");
            Long reviewCount = (Long) result.get("reviewCount");
            
            AdminProductDTO dto = AdminProductDTO.fromEntityWithRating(product, averageRating, reviewCount);
            
            return ResponseEntity.ok(ResponseWrapper.success(dto));
        } catch (Exception e) {
            log.error("Error fetching product {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi tải sản phẩm: " + e.getMessage()));
        }
    }
    
    /**
     * Get all catalogs for dropdown
     */
    @GetMapping("/api/catalogs")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<List<Catalog>>> getCatalogs() {
        try {
            List<Catalog> catalogs = catalogRepository.findAll();
            return ResponseEntity.ok(ResponseWrapper.success(catalogs));
        } catch (Exception e) {
            log.error("Error fetching catalogs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error("Lỗi khi tải danh mục: " + e.getMessage()));
        }
    }

    // Removed placeholder duplicate export endpoint to avoid ambiguous mapping

    /**
     * Generate product description using AI
     */
    @PostMapping("/api/generate-description")
    @ResponseBody
    public ResponseEntity<ResponseWrapper<String>> generateDescription(
            @RequestParam String productName,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(defaultValue = "") String keywords) {
        
        // Validate productName không rỗng
        if (productName == null || productName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(ResponseWrapper.error("Vui lòng nhập tên sản phẩm"));
        }
        
        try {
            String description = adminProductAiService.generateProductDescription(
                productName.trim(), catalogId, keywords
            );
            return ResponseEntity.ok(ResponseWrapper.success(description, 
                "Tạo mô tả thành công"));
        } catch (Exception e) {
            log.error("Error generating description for product: {}", productName, e);
            
            // Determine error type and return appropriate message
            String errorMessage = "Không thể tạo mô tả. Vui lòng thử lại.";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout")) {
                    errorMessage = "AI đang quá tải, vui lòng thử lại";
                } else if (e.getMessage().contains("quota") || e.getMessage().contains("Quota")) {
                    errorMessage = "Đã vượt giới hạn sử dụng AI hôm nay";
                } else if (e.getMessage().contains("network") || e.getMessage().contains("Network")) {
                    errorMessage = "Không thể kết nối với AI";
                }
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseWrapper.error(errorMessage));
        }
    }
}
