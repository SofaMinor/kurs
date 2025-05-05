package com.clinic.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medication_orders")
public class MedicationOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(nullable = false)
    private int quantityOrdered; // Сколько заказано

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private String status = "PENDING"; // Статус: PENDING, FULFILLED, CANCELLED
}