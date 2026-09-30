package com.example.demo.repository;

import com.example.demo.entity.Salary;
import com.example.demo.entity.User;
import com.example.demo.entity.enums.SalaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {
    
    /**
     * Find salary by user and month/year
     */
    Optional<Salary> findByUserAndMonthYear(User user, LocalDate monthYear);
    
    /**
     * Find all salaries for a user
     */
    List<Salary> findByUserOrderByMonthYearDesc(User user);
    
    /**
     * Find salaries by status
     */
    List<Salary> findByStatus(SalaryStatus status);
    
    /**
     * Find salary for current month by user ID
     */
    @Query("SELECT s FROM Salary s WHERE s.user.id = :userId " +
           "AND YEAR(s.monthYear) = YEAR(CURRENT_DATE) " +
           "AND MONTH(s.monthYear) = MONTH(CURRENT_DATE)")
    Optional<Salary> findCurrentMonthSalaryByUserId(@Param("userId") Long userId);
    
    /**
     * Find salary by user ID and specific month/year
     */
    @Query("SELECT s FROM Salary s WHERE s.user.id = :userId " +
           "AND YEAR(s.monthYear) = :year " +
           "AND MONTH(s.monthYear) = :month")
    Optional<Salary> findByUserIdAndMonthAndYear(@Param("userId") Long userId,
                                                  @Param("month") Integer month,
                                                  @Param("year") Integer year);
    
    /**
     * Find salaries by user ID and year/month range
     */
    @Query("SELECT s FROM Salary s WHERE s.user.id = :userId " +
           "AND s.monthYear BETWEEN :startDate AND :endDate " +
           "ORDER BY s.monthYear DESC")
    List<Salary> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    // Count salaries by user ID (for deletion check)
    long countByUserId(Long userId);
}
