package com.example.demo.controller;

import com.example.demo.entity.Catalog;
import com.example.demo.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "📂 Catalogs", description = "Product category management APIs")
@RestController
@RequestMapping("/api/catalogs")
@RequiredArgsConstructor
public class CatalogController {
    
    private final CatalogService catalogService;
    
    /**
     * Get all catalogs (Public)
     */
    @Operation(summary = "Get all catalogs", description = "Retrieve all product catalogs (public)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catalogs retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Error retrieving catalogs")
    })
    @GetMapping
    public ResponseEntity<List<Catalog>> getAllCatalogs() {
        return ResponseEntity.ok(catalogService.findAll());
    }
    
    /**
     * Get catalog by ID (Public)
     */
    @Operation(summary = "Get catalog by ID", description = "Retrieve a catalog by ID (public)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catalog retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Catalog not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Catalog> getCatalogById(
            @Parameter(description = "Catalog ID", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(catalogService.findById(id));
    }
    
    /**
     * Create new catalog (Admin only)
     */
    @Operation(summary = "Create catalog", description = "Create a new product catalog (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catalog created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid catalog data")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCatalog(@RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            if (value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên danh mục không được để trống");
            }
            String image = request.get("image");
            return ResponseEntity.ok(catalogService.create(value.trim(), image));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Update catalog (Admin only)
     */
    @Operation(summary = "Update catalog", description = "Update an existing product catalog (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catalog updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid catalog data"),
        @ApiResponse(responseCode = "404", description = "Catalog not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCatalog(
            @Parameter(description = "Catalog ID", required = true) @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            if (value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên danh mục không được để trống");
            }
            String image = request.get("image");
            return ResponseEntity.ok(catalogService.update(id, value.trim(), image));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Update catalog image only (Admin only)
     */
    @Operation(summary = "Update catalog image", description = "Update the image of a product catalog (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catalog image updated successfully"),
        @ApiResponse(responseCode = "404", description = "Catalog not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Catalog> updateCatalogImage(
            @Parameter(description = "Catalog ID", required = true) @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String image = request.get("image");
        return ResponseEntity.ok(catalogService.updateImage(id, image));
    }
    
    /**
     * Delete catalog (Admin only)
     */
    @Operation(summary = "Delete catalog", description = "Delete a product catalog (admin only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Catalog deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Catalog not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCatalog(@Parameter(description = "Catalog ID", required = true) @PathVariable Long id) {
        try {
            catalogService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

