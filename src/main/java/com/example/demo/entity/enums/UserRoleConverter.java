package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for UserRole enum to handle case-insensitive mapping
 * between database string values and Java enum constants
 */
@Converter(autoApply = true)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole userRole) {
        if (userRole == null) {
            return null;
        }
        return userRole.name(); // Returns uppercase enum name (e.g., "CUSTOMER")
    }

    @Override
    public UserRole convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.trim().isEmpty()) {
            return UserRole.CUSTOMER; // default value
        }
        
        return UserRole.fromString(dbValue.trim());
    }
}
