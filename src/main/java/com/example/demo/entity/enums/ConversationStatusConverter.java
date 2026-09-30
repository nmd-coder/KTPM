package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for ConversationStatus enum
 * Converts between enum and database string representation
 */
@Converter(autoApply = true)
public class ConversationStatusConverter implements AttributeConverter<ConversationStatus, String> {

    @Override
    public String convertToDatabaseColumn(ConversationStatus status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public ConversationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return ConversationStatus.fromString(dbData);
    }
}

