package com.example.demo.repository;

import com.example.demo.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query; // Thêm import này
import org.springframework.data.repository.query.Param; // Thêm import này
import java.util.Set; // Thêm import này

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    
    Optional<Follow> findByUserIdAndProductId(Long userId, Long productId);
    
    List<Follow> findByUserId(Long userId);
    
    List<Follow> findByProductId(Long productId);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.product.id = :productId")
    Long countFollowersByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT f.product.id FROM Follow f WHERE f.user.id = :userId")
    Set<Long> findProductIdsByUserId(@Param("userId") Long userId);
    
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
