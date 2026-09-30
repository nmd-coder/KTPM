package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for MessageType enum
 * Converts between enum and database string representation
 */
@Converter(autoApply = true)
public class MessageTypeConverter implements AttributeConverter<MessageType, String> {

    @Override
    public String convertToDatabaseColumn(MessageType type) {
        if (type == null) {
            return null;
        }
        return type.getValue();
    }

    @Override
    public MessageType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return MessageType.fromString(dbData);
    }
}

