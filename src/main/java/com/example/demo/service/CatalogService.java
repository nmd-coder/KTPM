package com.example.demo.service;

import com.example.demo.entity.Catalog;
import com.example.demo.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catalog Service for handling catalog business logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CatalogService {
    
    private final CatalogRepository catalogRepository;
    
    /**
     * Find all catalogs ordered by value
     * @return List of all catalogs
     */
    public List<Catalog> findAll() {
        log.info("Fetching all catalogs");
        return catalogRepository.findAllByOrderByValueAsc();
    }
    
    /**
     * Find catalog by ID
     * @param id Catalog ID
     * @return Catalog entity
     * @throws RuntimeException if not found
     */
    public Catalog findById(Long id) {
        log.info("Fetching catalog with ID: {}", id);
        return catalogRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Catalog not found with id: " + id));
    }
    
    /**
     * Find catalog by value
     * @param value Catalog value
     * @return Catalog entity
     * @throws RuntimeException if not found
     */
    public Catalog findByValue(String value) {
        log.info("Fetching catalog with value: {}", value);
        return catalogRepository.findByValue(value)
            .orElseThrow(() -> new RuntimeException("Catalog not found with value: " + value));
    }
    
    /**
     * Create new catalog
     * @param value Catalog value
     * @return Created catalog
     * @throws RuntimeException if catalog already exists
     */
    @Transactional
    public Catalog create(String value) {
        return create(value, null);
    }
    
    /**
     * Create new catalog with image
     * @param value Catalog value
     * @param image Catalog image path
     * @return Created catalog
     * @throws RuntimeException if catalog already exists
     */
    @Transactional
    public Catalog create(String value, String image) {
        log.info("Creating new catalog with value: {} and image: {}", value, image);
        
        if (catalogRepository.existsByValue(value)) {
            log.error("Catalog already exists with value: {}", value);
            throw new RuntimeException("Catalog already exists with value: " + value);
        }
        
        Catalog catalog = new Catalog();
        catalog.setValue(value);
        catalog.setImage(image);
        
        Catalog savedCatalog = catalogRepository.save(catalog);
        log.info("Successfully created catalog with ID: {}", savedCatalog.getId());
        
        return savedCatalog;
    }
    
    /**
     * Update catalog
     * @param id Catalog ID
     * @param value New catalog value
     * @return Updated catalog
     */
    @Transactional
    public Catalog update(Long id, String value) {
        return update(id, value, null);
    }
    
    /**
     * Update catalog with image
     * @param id Catalog ID
     * @param value New catalog value
     * @param image New catalog image path (null to keep existing)
     * @return Updated catalog
     */
    @Transactional
    public Catalog update(Long id, String value, String image) {
        log.info("Updating catalog ID: {} with new value: {} and image: {}", id, value, image);
        
        Catalog catalog = findById(id);
        
        // Check if value is changing and if new value already exists
        if (!catalog.getValue().equals(value) && catalogRepository.existsByValue(value)) {
            log.error("Catalog already exists with value: {}", value);
            throw new RuntimeException("Catalog already exists with value: " + value);
        }
        
        catalog.setValue(value);
        
        // Only update image if provided (not null)
        if (image != null) {
            catalog.setImage(image);
        }
        
        Catalog updatedCatalog = catalogRepository.save(catalog);
        log.info("Successfully updated catalog ID: {}", id);
        
        return updatedCatalog;
    }
    
    /**
     * Update only catalog image
     * @param id Catalog ID
     * @param image New catalog image path
     * @return Updated catalog
     */
    @Transactional
    public Catalog updateImage(Long id, String image) {
        log.info("Updating catalog ID: {} with new image: {}", id, image);
        
        Catalog catalog = findById(id);
        catalog.setImage(image);
        
        Catalog updatedCatalog = catalogRepository.save(catalog);
        log.info("Successfully updated catalog image for ID: {}", id);
        
        return updatedCatalog;
    }
    
    /**
     * Delete catalog
     * @param id Catalog ID
     * @throws RuntimeException if catalog has products
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting catalog ID: {}", id);
        
        Catalog catalog = findById(id);
        
        // Check if catalog has products
        if (!catalog.getProducts().isEmpty()) {
            log.error("Cannot delete catalog ID: {} - has {} products", id, catalog.getProducts().size());
            throw new RuntimeException("Không thể xóa danh mục này vì đang có " + catalog.getProducts().size() + " sản phẩm");
        }
        
        catalogRepository.delete(catalog);
        
        log.info("Successfully deleted catalog ID: {}", id);
    }
}

