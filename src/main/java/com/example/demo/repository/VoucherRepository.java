package com.example.demo.repository;

import com.example.demo.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    Optional<Voucher> findByCode(String code);
    
    @Query("SELECT v FROM Voucher v WHERE v.expiryDate >= :currentDate AND (v.maxUses IS NULL OR v.uses < v.maxUses) AND v.isActive = true")
    List<Voucher> findValidVouchers(@Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT v FROM Voucher v WHERE v.code = :code AND v.expiryDate >= :currentDate AND (v.maxUses IS NULL OR v.uses < v.maxUses) AND v.isActive = true")
    Optional<Voucher> findValidVoucherByCode(@Param("code") String code, @Param("currentDate") LocalDate currentDate);
    
    boolean existsByCode(String code);
    
    /**
     * Find all active vouchers that haven't expired yet
     */
    List<Voucher> findByIsActiveTrueAndExpiryDateAfter(LocalDate date);
}
