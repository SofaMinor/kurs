package com.clinic.management.service;

import com.clinic.management.entity.Doctor;
import com.clinic.management.entity.Schedule;
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public Schedule addSchedule(Schedule schedule) {
        if (schedule.getDoctor() != null && schedule.getDoctor().getId() != null) {
            Doctor doctor = doctorRepository.findById(schedule.getDoctor().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
            schedule.setDoctor(doctor);
        }
        return scheduleRepository.save(schedule);
    }

    public List<Schedule> getDoctorSchedule(Long doctorId) {
        return scheduleRepository.findByDoctorId(doctorId);
    }

    public void deleteSchedule(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    public List<Schedule> getAvailableSchedules(LocalDate date, String specialization) {
        return scheduleRepository.findAvailableSchedules(date, specialization);
    }
}