package com.clinic.management.repository;

import com.clinic.management.entity.MedicationOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationOrderRepository extends JpaRepository<MedicationOrder, Long> {}