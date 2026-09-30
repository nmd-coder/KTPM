package com.example.demo.service;

import com.example.demo.dto.address.AddressDto;
import com.example.demo.dto.address.AddressUpsertDto;
import com.example.demo.dto.location.DistrictDto;
import com.example.demo.dto.location.ProvinceDto;
import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AddressService {
    
    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);
    
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final LocationService locationService;
    
    public AddressService(AddressRepository addressRepository, UserRepository userRepository, LocationService locationService) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.locationService = locationService;
    }
    
    /**
     * Create or update address with OLD/NEW mode validation
     */
    public AddressDto createOrUpdateAddress(Long userId, AddressUpsertDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate address data based on mode
        validateAddressDto(dto);
        
        Address address;
        if (dto.id() != null) {
            // Update existing address
            address = addressRepository.findById(dto.id())
                    .orElseThrow(() -> new RuntimeException("Address not found"));
            
            // Verify ownership
            if (!address.getUser().getId().equals(userId)) {
                throw new RuntimeException("Address does not belong to user");
            }
            
            updateAddressFromDto(address, dto);
        } else {
            // Create new address
            address = createAddressFromDto(dto, user);
        }
        
        // Handle default address logic
        if (dto.isDefault() != null && dto.isDefault()) {
            handleDefaultAddress(userId, address);
        }
        
        address = addressRepository.save(address);
        logger.info("Address {} saved for user {} in {} mode", address.getId(), userId, dto.addressMode());
        
        return AddressDto.fromEntity(address);
    }
    
    /**
     * Get all addresses for user
     */
    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(Long userId) {
        List<Address> addresses = addressRepository.findByUserIdOrderByDefault(userId);
        return addresses.stream()
                .map(AddressDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get user's default address
     */
    @Transactional(readOnly = true)
    public Optional<AddressDto> getUserDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(AddressDto::fromEntity);
    }
    
    /**
     * Get GHN-compatible addresses for user
     */
    @Transactional(readOnly = true)
    public List<AddressDto> getGhnCompatibleAddresses(Long userId) {
        List<Address> addresses = addressRepository.findGhnCompatibleAddressesByUserId(userId);
        return addresses.stream()
                .map(AddressDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Delete address
     */
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        // Verify ownership
        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to user");
        }
        
        addressRepository.delete(address);
        logger.info("Address {} deleted for user {}", addressId, userId);
    }
    
    /**
     * Validate address DTO based on mode
     */
    private void validateAddressDto(AddressUpsertDto dto) {
        if (dto.isOldMode()) {
            // OLD mode: require province_id, district_id, ward_code, address_detail
            if (dto.provinceId() == null) {
                throw new IllegalArgumentException("Province ID is required for OLD mode");
            }
            if (dto.districtId() == null) {
                throw new IllegalArgumentException("District ID is required for OLD mode");
            }
            if (dto.wardCode() == null || dto.wardCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Ward code is required for OLD mode");
            }
            if (dto.addressDetail() == null || dto.addressDetail().trim().isEmpty()) {
                throw new IllegalArgumentException("Address detail is required for OLD mode");
            }
        } else if (dto.isNewMode()) {
            // NEW mode: require province_id, ward_code, address_detail (district_id optional)
            if (dto.provinceId() == null) {
                throw new IllegalArgumentException("Province ID is required for NEW mode");
            }
            if (dto.wardCode() == null || dto.wardCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Ward code is required for NEW mode");
            }
            if (dto.addressDetail() == null || dto.addressDetail().trim().isEmpty()) {
                throw new IllegalArgumentException("Address detail is required for NEW mode");
            }
        } else {
            throw new IllegalArgumentException("Invalid address mode. Must be 'OLD' or 'NEW'");
        }
    }
    
    /**
     * Create new address from DTO
     */
    private Address createAddressFromDto(AddressUpsertDto dto, User user) {
        Address address = new Address();
        address.setUser(user);
        updateAddressFromDto(address, dto);
        
        // Set default if this is the first address
        if (dto.isDefault() == null) {
            long existingCount = addressRepository.countDefaultAddressesByUserId(user.getId());
            address.setIsDefault(existingCount == 0);
        }
        
        return address;
    }
    
    /**
     * Update address fields from DTO
     */
    private void updateAddressFromDto(Address address, AddressUpsertDto dto) {
        // Set mode
        address.setAddressMode(dto.addressMode());
        
        // Set GHN fields
        address.setProvinceId(dto.provinceId());
        address.setDistrictId(dto.districtId());
        address.setWardCode(dto.wardCode());
        address.setAddressDetail(dto.addressDetail());
        address.setProvinceName(dto.provinceName());
        address.setDistrictName(dto.districtName());
        address.setWardName(dto.wardName());
        
        // Set legacy fields for backward compatibility
        if (dto.street() != null) address.setStreet(dto.street());
        if (dto.city() != null) address.setCity(dto.city());
        if (dto.province() != null) address.setProvince(dto.province());
        
        // Backfill legacy fields from GHN data if not provided
        if (address.getStreet() == null && dto.addressDetail() != null) {
            address.setStreet(dto.addressDetail());
        }
        
        // Ensure city is never null - required by database constraint
        if (address.getCity() == null) {
            if (dto.districtName() != null && !dto.districtName().trim().isEmpty()) {
                address.setCity(dto.districtName());
            } else if (dto.districtId() != null) {
                // Fetch district name from LocationService
                try {
                    DistrictDto district = locationService.findDistrictById(dto.districtId());
                    if (district != null) {
                        address.setCity(district.name());
                        address.setDistrictName(district.name());
                    } else {
                        // Fallback to province name if district not found
                        address.setCity(dto.provinceName() != null ? dto.provinceName() : "N/A");
                    }
                } catch (Exception e) {
                    logger.warn("Failed to fetch district name for ID {}: {}", dto.districtId(), e.getMessage());
                    address.setCity(dto.provinceName() != null ? dto.provinceName() : "N/A");
                }
            } else {
                // No district info available, use province name as fallback
                address.setCity(dto.provinceName() != null ? dto.provinceName() : "N/A");
            }
        }
        
        // Ensure province is never null - required by database constraint
        if (address.getProvince() == null) {
            if (dto.provinceName() != null && !dto.provinceName().trim().isEmpty()) {
                address.setProvince(dto.provinceName());
            } else if (dto.provinceId() != null) {
                // Fetch province name from LocationService
                try {
                    ProvinceDto province = locationService.findProvinceById(dto.provinceId());
                    if (province != null) {
                        address.setProvince(province.name());
                        address.setProvinceName(province.name());
                    } else {
                        address.setProvince("N/A");
                    }
                } catch (Exception e) {
                    logger.warn("Failed to fetch province name for ID {}: {}", dto.provinceId(), e.getMessage());
                    address.setProvince("N/A");
                }
            } else {
                address.setProvince("N/A");
            }
        }
        
        if (dto.isDefault() != null) {
            address.setIsDefault(dto.isDefault());
        }
    }
    
    /**
     * Handle default address logic - ensure only one default per user
     */
    private void handleDefaultAddress(Long userId, Address newDefaultAddress) {
        // Find current default address
        Optional<Address> currentDefault = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        
        if (currentDefault.isPresent() && !currentDefault.get().getId().equals(newDefaultAddress.getId())) {
            // Unset current default
            Address current = currentDefault.get();
            current.setIsDefault(false);
            addressRepository.save(current);
            logger.debug("Unset default flag for address {}", current.getId());
        }
        
        newDefaultAddress.setIsDefault(true);
    }
}
