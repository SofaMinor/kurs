package com.clinic.management.controller;

import com.clinic.management.entity.Medication;
// Убедитесь, что импорт правильный
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.service.MedicationService;
import org.slf4j.Logger; // Добавим логгер
import org.slf4j.LoggerFactory; // Добавим логгер
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Для сообщений после редиректа

@Controller
@RequestMapping("/medications") // Базовый путь для всех методов этого контроллера
public class MedicationController {

    private static final Logger log = LoggerFactory.getLogger(MedicationController.class);


    @Autowired
    private MedicationService medicationService;

    // Отображение страницы управления лекарствами (HTML шаблон)
    @GetMapping
    public String viewMedications(Model model) {
        log.info("GET request received for /medications");
        try {
            // Добавляем список ВСЕХ лекарств
            model.addAttribute("medications", medicationService.getAllMedications());
            // ИСПРАВЛЕНО: Добавляем список лекарств с НИЗКИМ ЗАПАСОМ
            model.addAttribute("lowStockMedications", medicationService.getLowStockMedications());
            // Добавляем пустой объект для формы добавления (если нужно использовать th:object)
            // model.addAttribute("newMedication", new Medication());
        } catch (Exception e) {
            log.error("Error loading medications page", e);
            // Можно добавить атрибут с ошибкой для отображения на спец. странице ошибки
            model.addAttribute("errorMessage", "Не удалось загрузить данные о лекарствах.");
            return "error"; // или вернуть имя шаблона с ошибкой
        }
        // Возвращаем имя HTML-файла (без расширения) из папки templates
        return "medications";
    }

    // Обработка добавления нового лекарства (из формы)
    @PostMapping // Метод POST на /medications
    public String addMedication(Medication medication, RedirectAttributes redirectAttributes) {
        // Medication medication - Spring сам создаст объект из полей формы,
        // если имена полей <input> совпадают с полями класса Medication
        log.info("POST request received for /medications to add: {}", medication.getName());
        try {
            Medication saved = medicationService.saveMedication(medication);
            redirectAttributes.addFlashAttribute("successMessage", "Препарат '" + saved.getName() + "' успешно добавлен/обновлен.");
        } catch (Exception e) {
            log.error("Error adding medication {}", medication.getName(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при добавлении препарата: " + e.getMessage());
        }
        return "redirect:/medications"; // Перенаправление обратно на страницу списка
    }

    // Обработка изменения запаса (из JS prompt, который перенаправляет сюда)
    // ИЗМЕНЕНО: Лучше использовать POST или PUT для изменений, но т.к. JS делает GET, оставим GET
    // Если бы JS делал POST, то использовали бы @PostMapping
    @GetMapping("/update-stock") // Метод GET на /medications/update-stock
    public String updateStock(@RequestParam Long id, @RequestParam int quantity, RedirectAttributes redirectAttributes) {
        log.info("GET request received for /medications/update-stock id={}, quantityChange={}", id, quantity);
        try {
            medicationService.updateMedicationStock(id, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Запас препарата успешно обновлен.");
        } catch (ResourceNotFoundException e) { // Используем существующий класс
            log.warn("Update stock failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IllegalArgumentException e) { // Обработка ошибки отрицательного количества
            log.warn("Update stock failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error updating stock for medication id={}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении запаса: " + e.getMessage());
        }
        return "redirect:/medications"; // Перенаправление обратно на страницу списка
    }

    // Обработка удаления лекарства (по ссылке)
    @GetMapping("/delete/{id}") // Используем GET для простоты (совпадает с HTML)
    public String deleteMedication(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("GET request received for /medications/delete/{}", id);
        try {
            medicationService.deleteMedication(id);
            redirectAttributes.addFlashAttribute("successMessage", "Препарат успешно удален.");
        } catch (ResourceNotFoundException e) { // Используем существующий класс
            log.warn("Delete failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting medication id={}", id, e);
            // Возможно, ошибка из-за внешних ключей (если лекарство используется где-то)
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить препарат. Возможно, он используется в записях.");
        }
        return "redirect:/medications"; // Перенаправление обратно на страницу списка
    }
}