package com.example.demo.config;

import com.example.demo.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import com.example.demo.entity.Catalog;

/**
 * Global Controller Advice to add common model attributes to all views
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalControllerAdvice {
    
    private final CatalogService catalogService;
    
    /**
     * Add catalogs to all views for header dropdown
     */
    @ModelAttribute("headerCatalogs")
    public List<Catalog> addCatalogsToModel() {
        try {
            return catalogService.findAll();
        } catch (Exception e) {
            log.error("Error loading catalogs for header: {}", e.getMessage());
            return List.of(); // Return empty list if error occurs
        }
    }
}
