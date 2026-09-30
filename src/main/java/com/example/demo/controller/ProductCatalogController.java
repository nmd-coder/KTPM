package com.example.demo.controller;

import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "🌸 Products by Catalog", description = "Get products filtered by catalog")
@RestController
@RequestMapping("/api/products/catalogs")
@RequiredArgsConstructor
public class ProductCatalogController {
    
    private final ProductService productService;
    
    /**
     * Get products by catalog ID
     * @param catalogId Catalog ID
     * @param page Page number (default: 0)
     * @param size Page size (default: 12)
     * @return Page of products
     */
    @Operation(summary = "Get products by catalog", description = "Retrieve products by catalog ID with pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Catalog not found")
    })
    @GetMapping("/{catalogId}")
    public ResponseEntity<Page<Product>> getProductsByCatalog(
            @Parameter(description = "Catalog ID", required = true) @PathVariable Long catalogId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "12") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.findByCatalog(catalogId, pageable));
    }
}

