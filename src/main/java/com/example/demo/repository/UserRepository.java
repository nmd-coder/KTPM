package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.UserRole;
import com.example.demo.dto.UserProfileDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhone(String phone);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    List<User> findByRole(UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.firstname LIKE %:keyword% OR u.lastname LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.firstname LIKE %:keyword% OR u.lastname LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchUsersWithAddresses(@Param("keyword") String keyword);
    
    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC")
    List<User> findRecentUsersByRole(@Param("role") UserRole role);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.email = :email")
    Optional<User> findByEmailWithAddresses(@Param("email") String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.email = :email")
    Optional<User> findByEmailWithOrders(@Param("email") String email);
    
    
    @Query("SELECT new com.example.demo.dto.UserProfileDTO(u, " +
           "(SELECT COUNT(o) FROM Order o WHERE o.user.id = u.id), " +
           "(SELECT COUNT(f) FROM Follow f WHERE f.user.id = u.id), " +
           "(SELECT COALESCE(SUM(ci.quantity), 0) FROM CartItem ci WHERE ci.cart.user.id = u.id)) " +
           "FROM User u WHERE u.email = :email")
    Optional<UserProfileDTO> findUserProfileByEmail(@Param("email") String email);
    
    // AI Customer Segmentation queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.customerSegment = :segment AND u.role = 'CUSTOMER'")
    Long countByCustomerSegment(@Param("segment") String segment);
    
    @Query("SELECT u FROM User u WHERE u.customerSegment = :segment AND u.role = 'CUSTOMER' ORDER BY u.createdAt DESC")
    List<User> findByCustomerSegment(@Param("segment") String segment);
    
    @Query("SELECT u FROM User u WHERE u.role = 'CUSTOMER'")
    List<User> findAllCustomers();
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders WHERE u.role = :role")
    List<User> findByRoleWithOrders(@Param("role") UserRole role);
}
