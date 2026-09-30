package com.example.demo.repository;

import com.example.demo.entity.DeliveryUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryUnitRepository extends JpaRepository<DeliveryUnit, Long> {
    
    Optional<DeliveryUnit> findByName(String name);
    
    boolean existsByName(String name);
}
