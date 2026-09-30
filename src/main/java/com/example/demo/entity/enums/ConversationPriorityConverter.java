package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for ConversationPriority enum
 * Converts between enum and database string representation
 */
@Converter(autoApply = true)
public class ConversationPriorityConverter implements AttributeConverter<ConversationPriority, String> {

    @Override
    public String convertToDatabaseColumn(ConversationPriority priority) {
        if (priority == null) {
            return null;
        }
        return priority.getValue();
    }

    @Override
    public ConversationPriority convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return ConversationPriority.fromString(dbData);
    }
}

