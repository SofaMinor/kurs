package com.clinic.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "medications")
public class Medication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int minStockLevel = 10;

    @Column(nullable = false)
    private BigDecimal price;
}