package com.clinic.management.controller;

import com.clinic.management.entity.Doctor;
import com.clinic.management.repository.DoctorRepository;
import com.clinic.management.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Добавить для PathVariable
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List; // Добавить для List
import java.util.Optional; // Добавить для Optional (лучше для поиска перед удалением)
import org.springframework.dao.EmptyResultDataAccessException; // Для отлова ошибки удаления несуществующего ID

@Controller
@RequestMapping("/doctors") // Базовый путь для врачей
public class DoctorController {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping("/add")
    public String showAddDoctorForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        return "add-doctor";
    }

    @PostMapping
    public String addDoctor(Doctor doctor, RedirectAttributes redirectAttributes) {
        try {
            if (doctor.getName() == null || doctor.getName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Имя врача не может быть пустым.");
                redirectAttributes.addFlashAttribute("doctor", doctor);
                return "redirect:/doctors/add";
            }


            doctorRepository.save(doctor);
            redirectAttributes.addFlashAttribute("successMessage", "Врач '" + doctor.getName() + "' успешно добавлен.");
            return "redirect:/doctors";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка добавления врача: " + e.getMessage());
            redirectAttributes.addFlashAttribute("doctor", doctor);
            return "redirect:/doctors/add";
        }
    }


    @GetMapping
    public String listDoctors(Model model) {
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            model.addAttribute("doctors", doctors);
            return "doctor";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Ошибка загрузки списка врачей: " + e.getMessage());
            model.addAttribute("doctors", List.of());
            return "doctor";
        }
    }


    @PostMapping("/delete/{id}")
    public String deleteDoctor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // ПРОВЕРКА ПЕРЕД УДАЛЕНИЕМ
            if (scheduleRepository.existsByDoctorId(id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Невозможно удалить врача (ID: " + id + "), так как у него есть записи в расписании. Сначала удалите или измените связанные расписания.");
                return "redirect:/doctors";
            }

            // Если связанных записей нет, продолжаем удаление
            Optional<Doctor> doctorOptional = doctorRepository.findById(id);
            if (doctorOptional.isPresent()) {
                doctorRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("successMessage", "Врач '" + doctorOptional.get().getName() + "' (ID: " + id + ") успешно удален.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Ошибка: Врач с ID " + id + " не найден.");
            }
        } catch (DataIntegrityViolationException e) { // Можно ловить более конкретное исключение
            // Эта ветка теперь менее вероятна при явной проверке, но оставим на всякий случай
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка целостности данных при удалении врача (ID: " + id + "). Возможно, остались связанные данные.");
        } catch (Exception e) {
            // ... остальная обработка ошибок ...
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления врача (ID: " + id + "): " + e.getMessage());
        }
        return "redirect:/doctors";
    }

}