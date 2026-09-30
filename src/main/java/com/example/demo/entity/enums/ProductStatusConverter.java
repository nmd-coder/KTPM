package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for ProductStatus enum to handle database value mapping
 * DEPRECATED: Use @Enumerated(EnumType.STRING) instead
 * autoApply disabled to prevent conflicts with @Enumerated
 */
@Converter(autoApply = false)
public class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductStatus productStatus) {
        if (productStatus == null) {
            return null;
        }
        return productStatus.getValue();
    }

    @Override
    public ProductStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        
        for (ProductStatus status : ProductStatus.values()) {
            if (status.getValue().equals(dbValue)) {
                return status;
            }
        }
        
        // Fallback: try to match by name (for backward compatibility)
        try {
            return ProductStatus.valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ProductStatus value: " + dbValue);
        }
    }
}
