package com.example.demo.service;

import com.example.demo.client.GhnClient;
import com.example.demo.dto.ghn.GhnDistrict;
import com.example.demo.dto.ghn.GhnProvince;
import com.example.demo.dto.ghn.GhnWard;
import com.example.demo.dto.location.DistrictDto;
import com.example.demo.dto.location.ProvinceDto;
import com.example.demo.dto.location.WardDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    
    private final GhnClient ghnClient;
    
    public LocationService(GhnClient ghnClient) {
        this.ghnClient = ghnClient;
    }
    
    /**
     * Check if GHN configuration is valid
     */
    public boolean isGhnConfigurationValid() {
        return ghnClient.isConfigurationValid();
    }
    
    /**
     * Get all provinces (cached for 1 hour)
     */
    @Cacheable(value = "provinces", unless = "#result.isEmpty()")
    public List<ProvinceDto> getProvinces() {
        try {
            if (!ghnClient.isConfigurationValid()) {
                logger.warn("GHN configuration is invalid, returning empty provinces list");
                return Collections.emptyList();
            }
            
            List<GhnProvince> ghnProvinces = ghnClient.getProvinces();
            List<ProvinceDto> result = ghnProvinces.stream()
                    .map(this::mapToProvinceDto)
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} provinces from GHN", result.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error retrieving provinces from GHN", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get districts by province ID (cached for 1 hour)
     */
    @Cacheable(value = "districts", key = "#provinceId", unless = "#result.isEmpty()")
    public List<DistrictDto> getDistricts(int provinceId) {
        try {
            if (!ghnClient.isConfigurationValid()) {
                logger.warn("GHN configuration is invalid, returning empty districts list for province {}", provinceId);
                return Collections.emptyList();
            }
            
            List<GhnDistrict> ghnDistricts = ghnClient.getDistricts(provinceId);
            List<DistrictDto> result = ghnDistricts.stream()
                    .map(this::mapToDistrictDto)
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} districts for province {} from GHN", result.size(), provinceId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error retrieving districts for province {} from GHN", provinceId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get wards by district ID (cached for 1 hour)
     */
    @Cacheable(value = "wards", key = "#districtId", unless = "#result.isEmpty()")
    public List<WardDto> getWards(int districtId) {
        try {
            if (!ghnClient.isConfigurationValid()) {
                logger.warn("GHN configuration is invalid, returning empty wards list for district {}", districtId);
                return Collections.emptyList();
            }
            
            List<GhnWard> ghnWards = ghnClient.getWards(districtId);
            List<WardDto> result = ghnWards.stream()
                    .map(this::mapToWardDto)
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} wards for district {} from GHN", result.size(), districtId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error retrieving wards for district {} from GHN", districtId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all wards by province ID (for 2-level address mode)
     * This loads all districts in the province and then all wards in those districts
     */
    @Cacheable(value = "wardsByProvince", key = "#provinceId", unless = "#result.isEmpty()")
    public List<WardDto> getWardsByProvince(int provinceId) {
        try {
            if (!ghnClient.isConfigurationValid()) {
                logger.warn("GHN configuration is invalid, returning empty wards list for province {}", provinceId);
                return Collections.emptyList();
            }
            
            // First get all districts in the province
            List<DistrictDto> districts = getDistricts(provinceId);
            if (districts.isEmpty()) {
                logger.warn("No districts found for province {}", provinceId);
                return Collections.emptyList();
            }
            
            // Then get all wards from all districts
            List<WardDto> allWards = districts.stream()
                    .flatMap(district -> getWards(district.id()).stream())
                    .collect(Collectors.toList());
            
            logger.info("Retrieved {} wards for province {} from GHN", allWards.size(), provinceId);
            return allWards;
            
        } catch (Exception e) {
            logger.error("Error retrieving wards for province {} from GHN", provinceId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Find province by ID
     */
    public ProvinceDto findProvinceById(int provinceId) {
        return getProvinces().stream()
                .filter(province -> province.id() == provinceId)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find district by ID
     */
    public DistrictDto findDistrictById(int districtId) {
        // We need to search across all provinces since we don't know which province this district belongs to
        return getProvinces().stream()
                .flatMap(province -> getDistricts(province.id()).stream())
                .filter(district -> district.id() == districtId)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find ward by code
     */
    public WardDto findWardByCode(String wardCode) {
        // We need to search across all districts since we don't know which district this ward belongs to
        return getProvinces().stream()
                .flatMap(province -> getDistricts(province.id()).stream())
                .flatMap(district -> getWards(district.id()).stream())
                .filter(ward -> ward.code().equals(wardCode))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if GHN service is available
     */
    public boolean isGhnServiceAvailable() {
        return ghnClient.isConfigurationValid();
    }
    
    // Mapping methods
    private ProvinceDto mapToProvinceDto(GhnProvince ghnProvince) {
        return new ProvinceDto(ghnProvince.provinceId(), ghnProvince.provinceName());
    }
    
    private DistrictDto mapToDistrictDto(GhnDistrict ghnDistrict) {
        return new DistrictDto(ghnDistrict.districtId(), ghnDistrict.districtName(), ghnDistrict.provinceId());
    }
    
    private WardDto mapToWardDto(GhnWard ghnWard) {
        return new WardDto(ghnWard.wardCode(), ghnWard.wardName(), ghnWard.districtId());
    }
}
