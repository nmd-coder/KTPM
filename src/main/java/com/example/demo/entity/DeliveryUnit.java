package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deliveryunits")
public class DeliveryUnit extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;


    @Column(name = "estimated_time", length = 50)
    private String estimatedTime;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "deliveryUnit", fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    // Constructors
    public DeliveryUnit() {
    }

    public DeliveryUnit(String name, String estimatedTime) {
        this.name = name;
        this.estimatedTime = estimatedTime;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(String estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
