package com.example.demo.entity;

import com.example.demo.entity.enums.SalaryStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Salaries", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "month_year"}))
public class Salary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "timesheets", "salaries"})
    private User user;

    @Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "working_time", nullable = false, precision = 5, scale = 2)
    private BigDecimal workingTime = BigDecimal.ZERO;

    @Column(name = "total_salary", nullable = false, precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalSalary;

    @Column(name = "month_year", nullable = false)
    private LocalDate monthYear;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SalaryStatus status = SalaryStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public Salary() {
    }

    public Salary(User user, BigDecimal baseSalary, LocalDate monthYear) {
        this.user = user;
        this.baseSalary = baseSalary;
        this.monthYear = monthYear;
    }

    // Helper methods
    public void calculateTotalSalary() {
        this.totalSalary = baseSalary.multiply(workingTime);
    }

    public boolean isPaid() {
        return status == SalaryStatus.PAID;
    }

    public boolean isOverdue() {
        return status == SalaryStatus.OVERDUE;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
        calculateTotalSalary();
    }

    public BigDecimal getWorkingTime() {
        return workingTime;
    }

    public void setWorkingTime(BigDecimal workingTime) {
        this.workingTime = workingTime;
        calculateTotalSalary();
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }

    public LocalDate getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(LocalDate monthYear) {
        this.monthYear = monthYear;
    }

    public SalaryStatus getStatus() {
        return status;
    }

    public void setStatus(SalaryStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
