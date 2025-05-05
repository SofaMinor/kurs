package com.clinic.management.repository;

import com.clinic.management.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDoctorId(Long doctorId);
    boolean existsByDoctorId(Long doctorId);

    @Query("SELECT s FROM Schedule s WHERE DATE(s.startTime) = :date " +
            "AND s.doctor.specialization = :specialization AND s.isAvailable = true")
    List<Schedule> findAvailableSchedules(LocalDate date, String specialization);

    List<Schedule> findAll();
}