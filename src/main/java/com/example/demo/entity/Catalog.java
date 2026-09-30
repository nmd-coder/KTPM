package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Catalogs")
public class Catalog extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String value;

    @Column(length = 255)
    private String image;

    @JsonIgnore
    @OneToMany(mappedBy = "catalog", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    // Constructors
    public Catalog() {
    }

    public Catalog(String value) {
        this.value = value;
    }

    // Getters and Setters
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

