
package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.WishlistService;
import com.example.demo.entity.Review;
import com.example.demo.dto.ProductDetailDTO;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.CatalogRepository;
import com.example.demo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product Controller for handling product-related web requests
 * Following rules.mdc specifications for presentation tier
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "🌸 Products", description = "Product APIs - Search, list, and view product details")
public class ProductController {

    private final ProductService productService;
    private final ReviewRepository reviewRepository;
    private final WishlistService wishlistService;
    private final UserRepository userRepository;
    private final CatalogRepository catalogRepository;

    /**
     * Display products listing page with optional filtering and sorting
     * @param page Page number (default: 0)
     * @param size Page size (default: 12)
     * @param sort Sort criteria (default: newest)
     * @param direction Sort direction (default: desc)
     * @param search Search keyword
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param categoryId Category ID filter
     * @param model Spring Model
     * @return Products listing template
     */
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            
            Model model,
            jakarta.servlet.http.HttpServletResponse response,
            Authentication authentication) {
        
        // Force no cache for this page
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        try {
            log.info("Products listing request - page: {}, size: {}, sort: {}, search: '{}', categoryId: {}", 
                    page, size, sort, search, categoryId);

            // 1. Setup Pagination
            int validPage = Math.max(0, page);
            int validSize = Math.min(Math.max(1, size), 50);
            
            // 2. Apply Combined Filtering Logic (Category AND Search)
            boolean isCatalogFilter = categoryId != null && categoryId > 0;
            boolean isSearchFilter = search != null && !search.trim().isEmpty();
            
            // ✅ NEW LOGIC: Khi filter theo category cụ thể → Không phân trang (lấy tất cả)
            Pageable pageable;
            if (isCatalogFilter) {
                // Lấy TẤT CẢ sản phẩm của category (max 1000)
                pageable = PageRequest.of(0, 1000);
                log.info("Category filter active - disabling pagination, fetching all products");
            } else {
                // Phân trang bình thường cho "Tất cả danh mục"
                pageable = PageRequest.of(validPage, validSize);
            }

            Page<Product> productsPage;

            if (isCatalogFilter || isSearchFilter) {
                // Use combined filter method for both catalog AND search
                productsPage = productService.findByCatalogAndSearch(categoryId, search, pageable);
                
                if (isCatalogFilter) {
                    model.addAttribute("categoryId", categoryId);
                }
                if (isSearchFilter) {
                    model.addAttribute("searchQuery", search);
                }

                if (isCatalogFilter && isSearchFilter) {
                    log.info("Combined filter: Category ID {} AND Search '{}', found {} products", 
                            categoryId, search, productsPage.getTotalElements());
                } else if (isCatalogFilter) {
                    log.info("Filter by category ID: {}, found {} products", categoryId, productsPage.getTotalElements());
                } else {
                    log.info("Search performed for: '{}', found {} products", search, productsPage.getTotalElements());
                }
                
            } else {
                // Default sorting
                productsPage = productService.getProductsSorted(sort, direction, pageable);
            }

            // Apply price filter if provided (for display purposes, actual filtering would need custom query)
            if (minPrice != null || maxPrice != null) {
                model.addAttribute("minPrice", minPrice);
                model.addAttribute("maxPrice", maxPrice);
            }

            // Get products with rating information
            Map<Long, ProductService.ProductWithRating> productsWithRatings = 
                productService.getProductsWithRatings(sort, direction, pageable);

            // Add model attributes
            model.addAttribute("productsPage", productsPage);
            model.addAttribute("products", productsPage.getContent());
            model.addAttribute("productsWithRatings", productsWithRatings);
            model.addAttribute("currentPage", validPage);
            model.addAttribute("totalPages", productsPage.getTotalPages());
            model.addAttribute("totalElements", productsPage.getTotalElements());
            model.addAttribute("pageSize", validSize);
            model.addAttribute("sortBy", sort);
            model.addAttribute("sortDirection", direction);
            
            // ✅ NEW: Flag để Frontend biết có phân trang không
            // Chỉ phân trang khi KHÔNG filter theo category cụ thể
            boolean isPaginationEnabled = !isCatalogFilter;
            model.addAttribute("isPaginationEnabled", isPaginationEnabled);
            
            // 4. Load All Catalogs for Filter Dropdown
            model.addAttribute("catalogs", catalogRepository.findAll());

            // 5. Wishlist product IDs for current user (for SSR wishlist state)
            try {
                if (authentication != null && authentication.isAuthenticated()) {
                    userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                        var ids = wishlistService.getWishlistProductIds(user.getId());
                        model.addAttribute("wishlistProductIds", new java.util.HashSet<>(ids));
                    });
                } else {
                    model.addAttribute("wishlistProductIds", java.util.Collections.emptySet());
                }
            } catch (Exception ex) {
                model.addAttribute("wishlistProductIds", java.util.Collections.emptySet());
            }
            
            // 6. Pagination helper attributes
            model.addAttribute("hasPrevious", productsPage.hasPrevious());
            model.addAttribute("hasNext", productsPage.hasNext());
            model.addAttribute("isFirst", productsPage.isFirst());
            model.addAttribute("isLast", productsPage.isLast());

            // 7. Set page metadata
            String pageTitle = "Tất cả sản phẩm";
            String pageDescription = "Khám phá bộ sưu tập hoa tươi đa dạng tại StarShop. Hoa sinh nhật, hoa tình yêu, hoa cưới và nhiều loại hoa khác với chất lượng tốt nhất.";

            if (search != null && !search.trim().isEmpty()) {
                pageTitle = "Tìm kiếm: " + search;
            } else if (categoryId != null) {
                catalogRepository.findById(categoryId).ifPresent(c -> 
                    model.addAttribute("categoryName", c.getValue())
                );
                pageTitle = (String) model.getAttribute("categoryName") + " | Danh mục sản phẩm";
            }
            
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("pageDescription", pageDescription);
            
            // 8. Add page-specific JavaScript
            model.addAttribute("additionalJS", List.of("/js/products.js"));

            log.info("Products listing completed - showing {} products on page {}/{}", 
                    productsPage.getContent().size(), validPage + 1, productsPage.getTotalPages());

            return "products/index";

        } catch (Exception e) {
            log.error("Error loading products listing: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải danh sách sản phẩm. Vui lòng thử lại sau.");
            return "error/500";
        }
    }

    /**
     * Display single product detail page
     * @param id Product ID
     * @param model Spring Model
     * @return Product detail template
     */
    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model,Authentication authentication) {
        try {
            log.info("Product detail request for ID: {}", id);

            // Validate product ID
            if (id == null || id <= 0) {
                log.warn("Invalid product ID: {}", id);
                model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
                return "error/404";
            }

            // Get product with rating information using optimized query
            ProductDetailDTO productDetail = productService.getProductWithRating(id);
            if (productDetail == null || productDetail.getProduct() == null) {
                log.warn("Product not found with ID: {}", id);
                model.addAttribute("errorMessage", "Sản phẩm không tồn tại.");
                return "error/404";
            }

            Product product = productDetail.getProduct();
            
            // Get product reviews with pagination (eager fetch user and admin data)
            Pageable reviewPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Review> reviewsPage = reviewRepository.findByProductIdWithUser(id, reviewPageable);

            // Get related products (same category or similar)
            List<Product> relatedProducts = productService.getLatestProducts(4);
            
            // Remove current product from related products
            relatedProducts.removeIf(p -> p.getId().equals(id));
            if (relatedProducts.size() > 3) {
                relatedProducts = relatedProducts.subList(0, 3);
            }

            // Add model attributes
            model.addAttribute("product", product);
            model.addAttribute("averageRating", productDetail.getAverageRating());
            model.addAttribute("reviewCount", productDetail.getReviewCount());
            model.addAttribute("reviews", reviewsPage.getContent());
            model.addAttribute("reviewsPage", reviewsPage);
            model.addAttribute("relatedProducts", relatedProducts);
             // 5. Wishlist product IDs for current user (for SSR wishlist state)
            // Always initialize to prevent null pointer exceptions
            model.addAttribute("wishlistProductIds", java.util.Collections.emptySet());
            
            try {
                if (authentication != null && authentication.isAuthenticated()) {
                    userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                        var ids = wishlistService.getWishlistProductIds(user.getId());
                        model.addAttribute("wishlistProductIds", new java.util.HashSet<>(ids));
                    });
                }
            } catch (Exception ex) {
                log.error("Error loading wishlist for product detail: {}", ex.getMessage());
            }

            // Breadcrumbs removed - no longer extending BaseController

            // Set page metadata
            model.addAttribute("pageTitle", product.getName());
            model.addAttribute("pageDescription", product.getDescription() != null ? 
                    product.getDescription().substring(0, Math.min(product.getDescription().length(), 160)) : 
                    "Chi tiết sản phẩm " + product.getName() + " tại StarShop");
            
            // Add page-specific JavaScript
            model.addAttribute("additionalJS", List.of("/js/products.js"));

            log.info("Product detail loaded successfully for: {} (ID: {})", product.getName(), id);

            return "products/detail";

        } catch (Exception e) {
            log.error("Error loading product detail for ID {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải thông tin sản phẩm. Vui lòng thử lại sau.");
            return "error/500";
        }
    }

    /**
     * Search products endpoint for AJAX requests
     * @param q Search query
     * @param page Page number
     * @param size Page size
     * @return JSON response with products
     */
    @Operation(
        summary = "Tìm kiếm sản phẩm (AJAX)",
        description = "API tìm kiếm sản phẩm theo từ khóa, hỗ trợ phân trang. Dùng cho AJAX requests từ frontend."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tìm kiếm thành công, trả về danh sách sản phẩm")
    })
    @GetMapping("/search")
    @ResponseBody
    public Page<Product> searchProducts(
            @Parameter(description = "Từ khóa tìm kiếm", example = "hoa hồng")
            @RequestParam(required = false) String q,
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số sản phẩm mỗi trang (tối đa 50)", example = "12")
            @RequestParam(defaultValue = "12") int size) {
        
        try {
            log.info("AJAX search request - query: '{}', page: {}, size: {}", q, page, size);
            
            int validPage = Math.max(0, page);
            int validSize = Math.min(Math.max(1, size), 50);
            Pageable pageable = PageRequest.of(validPage, validSize);
            
            if (q != null && !q.trim().isEmpty()) {
                return productService.searchProducts(q.trim(), pageable);
            } else {
                return productService.findAll(pageable);
            }
            
        } catch (Exception e) {
            log.error("Error in AJAX search: {}", e.getMessage(), e);
            // Return empty page on error
            return Page.empty();
        }
    }

    /**
     * Get product categories page
     * @param model Spring Model
     * @return Categories page
     */
    @GetMapping("/categories")
    public String categories(Model model) {
        log.info("Categories page request");
        
        // Load all categories
        model.addAttribute("categories", catalogRepository.findAll());
        
        // Set page metadata
        model.addAttribute("pageTitle", "Danh mục sản phẩm");
        model.addAttribute("pageDescription", "Khám phá các danh mục hoa tươi đa dạng tại StarShop");

        log.info("Categories page loaded successfully");
        return "products/categories";
    }
    
}
