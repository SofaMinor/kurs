package com.clinic.management.controller;

import com.clinic.management.entity.Doctor;
import com.clinic.management.service.AppointmentService;
import com.clinic.management.service.DoctorService;
import com.clinic.management.service.MedicationService;
import com.clinic.management.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    private final ScheduleService scheduleService;
    private final MedicationService medicationService;
    private final AppointmentService appointmentService;
    private final DoctorService doctorService;


    public DashboardController(ScheduleService scheduleService, MedicationService medicationService, AppointmentService appointmentService, DoctorService doctorService) {
        this.scheduleService = scheduleService;
        this.medicationService = medicationService;
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;

    }

    @GetMapping
    public String showDashboard(Model model) {
        long doctorCount = doctorService.countAllDoctors();
        long medicationCount = medicationService.getAllMedications().size();
        long appointmentCountToday = appointmentService.countAllAppointments();
        model.addAttribute("pageTitle", "Панель управления"); // Set the page title
        model.addAttribute("doctorCount", doctorCount);
        model.addAttribute("medicationCount", medicationCount);
        model.addAttribute("appointmentCount", appointmentCountToday);
        return "dashboard";
    }
}