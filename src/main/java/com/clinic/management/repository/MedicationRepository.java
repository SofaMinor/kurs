package com.clinic.management.repository;

import com.clinic.management.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    List<Medication> findByQuantityLessThan(int minStockLevel);
}