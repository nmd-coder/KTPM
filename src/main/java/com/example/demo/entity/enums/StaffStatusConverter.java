package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for StaffStatus enum
 * Converts between enum and database VARCHAR
 */
@Converter(autoApply = true)
public class StaffStatusConverter implements AttributeConverter<StaffStatus, String> {

    @Override
    public String convertToDatabaseColumn(StaffStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public StaffStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return StaffStatus.fromString(dbData);
    }
}

