package com.clinic.management.config;

import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Medication;
import com.clinic.management.entity.Schedule;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.MedicationRepository;
import com.clinic.management.repository.ScheduleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(DoctorRepository doctorRepository,
                                   MedicationRepository medicationRepository,
                                   ScheduleRepository scheduleRepository) {
        return args -> {

        };
    }
}