package com.example.demo.repository;

import com.example.demo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByUserId(Long userId);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.isDefault DESC, a.id DESC")
    List<Address> findByUserIdOrderByDefault(@Param("userId") Long userId);
    
    void deleteByIdAndUserId(Long id, Long userId);
    
    // GHN-specific queries
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.addressMode = :mode")
    List<Address> findByUserIdAndAddressMode(@Param("userId") Long userId, @Param("mode") String mode);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
    Optional<Address> findByUserIdAndIsDefaultTrue(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
    long countDefaultAddressesByUserId(@Param("userId") Long userId);
    
    // Find GHN-compatible addresses
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.provinceId IS NOT NULL AND a.wardCode IS NOT NULL AND a.addressDetail IS NOT NULL")
    List<Address> findGhnCompatibleAddressesByUserId(@Param("userId") Long userId);
}
