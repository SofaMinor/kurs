package com.clinic.management.controller;

import com.clinic.management.entity.Doctor; // Добавить импорт
import com.clinic.management.entity.Schedule;
import com.clinic.management.repository.DoctorRepository; // Добавить импорт
import com.clinic.management.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat; // Добавить импорт для дат
import java.time.LocalDateTime; // Добавить импорт для дат
import java.util.List; // Добавить импорт для List

@Controller
@RequestMapping("/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired // Добавить инъекцию репозитория врачей
    private DoctorRepository doctorRepository;

    @GetMapping
    public String viewSchedules(Model model) {
        List<Schedule> schedules = scheduleService.getAllSchedules();
        List<Doctor> doctors = doctorRepository.findAll(); // Получить список врачей
        model.addAttribute("schedules", schedules);
        model.addAttribute("doctors", doctors); // Добавить врачей в модель
        // model.addAttribute("newSchedule", new Schedule()); // Можно добавить для th:object, но текущая форма использует @RequestParam
        return "schedules"; // Имя вашего HTML шаблона
    }

    // --- ИЗМЕНЕНО: Улучшенный метод добавления с @RequestParam ---
    // Использование @ModelAttribute Schedule schedule было бы проще,
    // но требует правильной настройки конвертера для Doctor ID -> Doctor entity.
    // Поэтому используем @RequestParam для явного получения ID.
    @PostMapping
    public String addSchedule(@RequestParam("doctor") Long doctorId, // Получаем ID врача
                              @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                              @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                              Model model) { // Добавляем Model для обработки ошибок

        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный ID врача: " + doctorId));

            Schedule newSchedule = new Schedule();
            newSchedule.setDoctor(doctor);
            newSchedule.setStartTime(startTime);
            newSchedule.setEndTime(endTime);
            newSchedule.setAvailable(true); // Новое расписание доступно по умолчанию

            scheduleService.addSchedule(newSchedule);
            return "redirect:/schedules"; // Перенаправление после успешного добавления
        } catch (Exception e) {
            // В случае ошибки, снова загружаем данные для формы и возвращаем ту же страницу
            // Добавляем сообщение об ошибке
            model.addAttribute("errorMessage", "Ошибка добавления расписания: " + e.getMessage());
            // Снова добавляем необходимые данные для рендеринга страницы
            List<Schedule> schedules = scheduleService.getAllSchedules();
            List<Doctor> doctors = doctorRepository.findAll();
            model.addAttribute("schedules", schedules);
            model.addAttribute("doctors", doctors);
            return "schedules"; // Возвращаем ту же страницу с ошибкой
        }
    }
    // --- КОНЕЦ ИЗМЕНЕНИЯ ---


    @GetMapping("/delete/{id}")
    public String deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return "redirect:/schedules";
    }
}