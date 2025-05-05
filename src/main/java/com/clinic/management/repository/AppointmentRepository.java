package com.clinic.management.repository;

import com.clinic.management.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorId(Long doctorId);
    // Найти записи по ID слота расписания
    boolean existsByScheduleId(Long scheduleId);
}