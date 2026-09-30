package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for IntentType enum
 * Converts between enum and database VARCHAR
 */
@Converter(autoApply = true)
public class IntentTypeConverter implements AttributeConverter<IntentType, String> {

    @Override
    public String convertToDatabaseColumn(IntentType intentType) {
        if (intentType == null) {
            return null;
        }
        return intentType.name();
    }

    @Override
    public IntentType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return IntentType.fromString(dbData);
    }
}

