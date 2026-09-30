package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Addresses")
public class Address extends BaseEntity {

    // Legacy fields (for backward compatibility)
    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    // GHN Address Integration fields
    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "district_id")
    private Integer districtId;

    @Column(name = "ward_code", length = 20)
    private String wardCode;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "province_name", length = 100)
    private String provinceName;

    @Column(name = "district_name", length = 100)
    private String districtName;

    @Column(name = "ward_name", length = 100)
    private String wardName;

    @Column(name = "address_mode", length = 8)
    private String addressMode = "OLD"; // OLD or NEW

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructors
    public Address() {
    }

    public Address(String street, String city, String province, Boolean isDefault, User user) {
        this.street = street;
        this.city = city;
        this.province = province;
        this.isDefault = isDefault;
        this.user = user;
        this.addressMode = "OLD";
    }

    // Constructor for GHN address (NEW mode)
    public Address(Integer provinceId, String wardCode, String addressDetail, 
                   String provinceName, String wardName, Boolean isDefault, User user) {
        this.provinceId = provinceId;
        this.wardCode = wardCode;
        this.addressDetail = addressDetail;
        this.provinceName = provinceName;
        this.wardName = wardName;
        this.isDefault = isDefault;
        this.user = user;
        this.addressMode = "NEW";
    }

    // Constructor for GHN address (OLD mode with district)
    public Address(Integer provinceId, Integer districtId, String wardCode, String addressDetail,
                   String provinceName, String districtName, String wardName, Boolean isDefault, User user) {
        this.provinceId = provinceId;
        this.districtId = districtId;
        this.wardCode = wardCode;
        this.addressDetail = addressDetail;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.wardName = wardName;
        this.isDefault = isDefault;
        this.user = user;
        this.addressMode = "OLD";
    }
    
    // Helper methods
    public String getFullAddress() {
        if ("NEW".equals(addressMode) || "OLD".equals(addressMode)) {
            // Use GHN format
            StringBuilder fullAddress = new StringBuilder();
            
            if (addressDetail != null && !addressDetail.trim().isEmpty()) {
                fullAddress.append(addressDetail.trim());
            }
            
            if (wardName != null && !wardName.trim().isEmpty()) {
                if (fullAddress.length() > 0) {
                    fullAddress.append(", ");
                }
                fullAddress.append(wardName.trim());
            }
            
            if (districtName != null && !districtName.trim().isEmpty()) {
                if (fullAddress.length() > 0) {
                    fullAddress.append(", ");
                }
                fullAddress.append(districtName.trim());
            }
            
            if (provinceName != null && !provinceName.trim().isEmpty()) {
                if (fullAddress.length() > 0) {
                    fullAddress.append(", ");
                }
                fullAddress.append(provinceName.trim());
            }
            
            return fullAddress.toString();
        } else {
            // Legacy format
            StringBuilder fullAddress = new StringBuilder();
            
            if (street != null && !street.trim().isEmpty()) {
                fullAddress.append(street.trim());
            }
            
            if (city != null && !city.trim().isEmpty()) {
                if (fullAddress.length() > 0) {
                    fullAddress.append(", ");
                }
                fullAddress.append(city.trim());
            }
            
            if (province != null && !province.trim().isEmpty()) {
                if (fullAddress.length() > 0) {
                    fullAddress.append(", ");
                }
                fullAddress.append(province.trim());
            }
            
            return fullAddress.toString();
        }
    }

    public boolean isGhnCompatible() {
        return ("NEW".equals(addressMode) && provinceId != null && wardCode != null && addressDetail != null) ||
               ("OLD".equals(addressMode) && provinceId != null && districtId != null && wardCode != null && addressDetail != null);
    }

    public boolean isNewMode() {
        return "NEW".equals(addressMode);
    }

    public boolean isOldMode() {
        return "OLD".equals(addressMode);
    }

    // Getters and Setters
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // GHN fields getters and setters
    public Integer getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Integer provinceId) {
        this.provinceId = provinceId;
    }

    public Integer getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Integer districtId) {
        this.districtId = districtId;
    }

    public String getWardCode() {
        return wardCode;
    }

    public void setWardCode(String wardCode) {
        this.wardCode = wardCode;
    }

    public String getAddressDetail() {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public String getAddressMode() {
        return addressMode;
    }

    public void setAddressMode(String addressMode) {
        this.addressMode = addressMode;
    }
}
