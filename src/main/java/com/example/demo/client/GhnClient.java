package com.example.demo.client;

import com.example.demo.config.GhnProperties;
import com.example.demo.dto.ghn.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class GhnClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GhnClient.class);
    
    private final RestTemplate restTemplate;
    private final GhnProperties ghnProperties;
    
    public GhnClient(RestTemplate restTemplate, GhnProperties ghnProperties) {
        this.restTemplate = restTemplate;
        this.ghnProperties = ghnProperties;
    }
    
    /**
     * Get all provinces from GHN
     */
    public List<GhnProvince> getProvinces() {
        try {
            String url = ghnProperties.getBaseUrl() + ghnProperties.getEndpoints().getProvinces();
            HttpHeaders headers = createHeaders(false);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            logger.info("=== Calling GHN provinces API ===");
            logger.info("URL: {}", url);
            logger.info("Token: {}...{}", 
                ghnProperties.getToken().substring(0, Math.min(10, ghnProperties.getToken().length())),
                ghnProperties.getToken().length() > 10 ? "..." : "");
            
            ResponseEntity<GhnProvinceResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, GhnProvinceResponse.class);
            
            logger.info("Response status: {}", response.getStatusCode());
            
            GhnProvinceResponse responseBody = response.getBody();
            if (responseBody != null && responseBody.data() != null) {
                logger.info("Successfully retrieved {} provinces from GHN", responseBody.data().size());
                return responseBody.data();
            }
            
            logger.warn("Empty response body from GHN provinces API");
            return Collections.emptyList();
            
        } catch (RestClientException e) {
            logger.error("Error calling GHN provinces API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get districts by province ID from GHN
     */
    public List<GhnDistrict> getDistricts(int provinceId) {
        try {
            String url = ghnProperties.getBaseUrl() + ghnProperties.getEndpoints().getDistricts() + "?province_id=" + provinceId;
            HttpHeaders headers = createHeaders(false);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            logger.debug("Calling GHN districts API: {}", url);
            ResponseEntity<GhnDistrictResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, GhnDistrictResponse.class);
            
            GhnDistrictResponse responseBody = response.getBody();
            if (responseBody != null && responseBody.data() != null) {
                logger.debug("Retrieved {} districts for province {} from GHN", 
                    responseBody.data().size(), provinceId);
                return responseBody.data();
            }
            
            logger.warn("Empty response from GHN districts API for province {}", provinceId);
            return Collections.emptyList();
            
        } catch (RestClientException e) {
            logger.error("Error calling GHN districts API for province {}", provinceId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get wards by district ID from GHN
     */
    public List<GhnWard> getWards(int districtId) {
        try {
            String url = ghnProperties.getBaseUrl() + ghnProperties.getEndpoints().getWards() + "?district_id=" + districtId;
            HttpHeaders headers = createHeaders(false);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            logger.debug("Calling GHN wards API: {}", url);
            ResponseEntity<GhnWardResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, GhnWardResponse.class);
            
            GhnWardResponse responseBody = response.getBody();
            if (responseBody != null && responseBody.data() != null) {
                logger.debug("Retrieved {} wards for district {} from GHN", 
                    responseBody.data().size(), districtId);
                return responseBody.data();
            }
            
            logger.warn("Empty response from GHN wards API for district {}", districtId);
            return Collections.emptyList();
            
        } catch (RestClientException e) {
            logger.error("Error calling GHN wards API for district {}", districtId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Calculate shipping fee from GHN
     */
    public GhnFeeResponse calculateShippingFee(GhnFeeRequest request) {
        try {
            String url = ghnProperties.getBaseUrl() + ghnProperties.getEndpoints().getCalculateFee();
            HttpHeaders headers = createHeaders(true); // Include ShopId for fee calculation
            HttpEntity<GhnFeeRequest> entity = new HttpEntity<>(request, headers);
            
            logger.debug("Calling GHN calculate fee API: {} with request: {}", url, request);
            ResponseEntity<GhnFeeResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, GhnFeeResponse.class);
            
            GhnFeeResponse responseBody = response.getBody();
            if (responseBody != null) {
                logger.debug("GHN fee calculation result: {}", responseBody);
                return responseBody;
            }
            
            logger.warn("Empty response from GHN calculate fee API");
            return new GhnFeeResponse("500", "Empty response", null);
            
        } catch (RestClientException e) {
            logger.error("Error calling GHN calculate fee API", e);
            return new GhnFeeResponse("500", "API call failed: " + e.getMessage(), null);
        }
    }
    
    /**
     * Create HTTP headers for GHN API calls
     */
    private HttpHeaders createHeaders(boolean includeShopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnProperties.getToken());
        
        if (includeShopId) {
            headers.set("ShopId", ghnProperties.getShopId());
        }
        
        return headers;
    }
    
    /**
     * Check if GHN configuration is valid
     */
    public boolean isConfigurationValid() {
        return ghnProperties.getToken() != null && !ghnProperties.getToken().isEmpty() &&
               ghnProperties.getShopId() != null && !ghnProperties.getShopId().isEmpty() &&
               ghnProperties.getBaseUrl() != null && !ghnProperties.getBaseUrl().isEmpty();
    }
    
    /**
     * Check if FROM location is configured (for shipping fee calculation)
     */
    public boolean isFromLocationConfigured() {
        return ghnProperties.getFrom().isComplete();
    }
}
