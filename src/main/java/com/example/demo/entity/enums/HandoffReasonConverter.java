package com.example.demo.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for HandoffReason enum
 * Converts between enum and database VARCHAR
 */
@Converter(autoApply = true)
public class HandoffReasonConverter implements AttributeConverter<HandoffReason, String> {

    @Override
    public String convertToDatabaseColumn(HandoffReason reason) {
        if (reason == null) {
            return null;
        }
        return reason.name();
    }

    @Override
    public HandoffReason convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return HandoffReason.fromString(dbData);
    }
}

