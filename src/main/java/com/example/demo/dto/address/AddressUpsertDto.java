package com.example.demo.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressUpsertDto(
    Long id, // null for create, not null for update
    
    @NotBlank(message = "Chế độ địa chỉ không được để trống")
    String addressMode, // "OLD" or "NEW"
    
    // Legacy fields (for OLD mode backward compatibility)
    String street,
    String city,
    String province,
    
    // GHN fields
    @NotNull(message = "Mã tỉnh/thành phố không được để trống")
    Integer provinceId,
    
    Integer districtId, // Optional for NEW mode, required for OLD mode
    
    @NotBlank(message = "Mã phường/xã không được để trống")
    String wardCode,
    
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    String addressDetail,
    
    String provinceName,
    String districtName,
    String wardName,
    
    Boolean isDefault
) {
    public boolean isNewMode() {
        return "NEW".equals(addressMode);
    }
    
    public boolean isOldMode() {
        return "OLD".equals(addressMode);
    }
}
