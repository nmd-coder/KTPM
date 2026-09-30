package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ghn")
public class GhnProperties {
    
    private String baseUrl;
    private String token;
    private String shopId;
    private Integer serviceTypeIdDefault;
    private From from = new From();
    private Endpoints endpoints = new Endpoints();
    
    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getShopId() {
        return shopId;
    }
    
    public void setShopId(String shopId) {
        this.shopId = shopId;
    }
    
    public Integer getServiceTypeIdDefault() {
        return serviceTypeIdDefault;
    }
    
    public void setServiceTypeIdDefault(Integer serviceTypeIdDefault) {
        this.serviceTypeIdDefault = serviceTypeIdDefault;
    }
    
    public From getFrom() {
        return from;
    }
    
    public void setFrom(From from) {
        this.from = from;
    }
    
    public Endpoints getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints;
    }
    
    // Nested classes
    public static class From {
        private String provinceId;
        private String districtId;
        private String wardCode;
        
        public String getProvinceId() {
            return provinceId;
        }
        
        public void setProvinceId(String provinceId) {
            this.provinceId = provinceId;
        }
        
        public String getDistrictId() {
            return districtId;
        }
        
        public void setDistrictId(String districtId) {
            this.districtId = districtId;
        }
        
        public String getWardCode() {
            return wardCode;
        }
        
        public void setWardCode(String wardCode) {
            this.wardCode = wardCode;
        }
        
        public boolean isComplete() {
            return provinceId != null && !provinceId.isEmpty() &&
                   districtId != null && !districtId.isEmpty() &&
                   wardCode != null && !wardCode.isEmpty();
        }
    }
    
    public static class Endpoints {
        private String provinces;
        private String districts;
        private String wards;
        private String calculateFee;
        
        public String getProvinces() {
            return provinces;
        }
        
        public void setProvinces(String provinces) {
            this.provinces = provinces;
        }
        
        public String getDistricts() {
            return districts;
        }
        
        public void setDistricts(String districts) {
            this.districts = districts;
        }
        
        public String getWards() {
            return wards;
        }
        
        public void setWards(String wards) {
            this.wards = wards;
        }
        
        public String getCalculateFee() {
            return calculateFee;
        }
        
        public void setCalculateFee(String calculateFee) {
            this.calculateFee = calculateFee;
        }
    }
}
