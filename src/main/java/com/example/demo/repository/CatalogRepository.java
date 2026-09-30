package com.example.demo.repository;

import com.example.demo.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long> {
    
    Optional<Catalog> findByValue(String value);
    
    boolean existsByValue(String value);
    
    List<Catalog> findAllByOrderByValueAsc();
}

