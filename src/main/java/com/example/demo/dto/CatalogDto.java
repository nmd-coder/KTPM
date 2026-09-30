package com.example.demo.dto;

import com.example.demo.entity.Catalog;

public record CatalogDto(Long id, String value, String image) {
    
    /**
     * Convert Catalog entity to DTO
     * @param catalog Catalog entity
     * @return CatalogDto or null if catalog is null
     */
    public static CatalogDto fromEntity(Catalog catalog) {
        if (catalog == null) {
            return null;
        }
        return new CatalogDto(
            catalog.getId(), 
            catalog.getValue(),
            catalog.getImage()
        );
    }
}

