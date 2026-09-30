package com.example.demo.dto.address;

import com.example.demo.entity.Address;

public record AddressDto(
    Long id,
    String street,
    String city,
    String province,
    Boolean isDefault,
    
    // GHN fields
    Integer provinceId,
    Integer districtId,
    String wardCode,
    String addressDetail,
    String provinceName,
    String districtName,
    String wardName,
    String addressMode,
    
    String fullAddress,
    boolean ghnCompatible
) {
    public static AddressDto fromEntity(Address address) {
        return new AddressDto(
            address.getId(),
            address.getStreet(),
            address.getCity(),
            address.getProvince(),
            address.getIsDefault(),
            address.getProvinceId(),
            address.getDistrictId(),
            address.getWardCode(),
            address.getAddressDetail(),
            address.getProvinceName(),
            address.getDistrictName(),
            address.getWardName(),
            address.getAddressMode(),
            address.getFullAddress(),
            address.isGhnCompatible()
        );
    }
}
