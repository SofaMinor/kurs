package com.clinic.management.controller;

import com.clinic.management.entity.Schedule;
import com.clinic.management.repository.ScheduleRepository; // Для получения деталей слота
import com.clinic.management.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/appointments") // Базовый URL для записей
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ScheduleRepository scheduleRepository; // Нужен для получения деталей слота


    // --- Страница поиска доступных слотов (упрощенный вариант) ---
    // В реальном приложении здесь была бы форма для выбора даты/врача
    // и, возможно, асинхронная загрузка слотов
    @GetMapping("/find")
    public String showAvailableSlots(Model model) {
        // Для примера показываем все доступные слоты на сегодня + 1 день
        // В реальном приложении дата должна приходить из запроса
        java.time.LocalDate today = java.time.LocalDate.now();
        List<Schedule> availableSchedules = appointmentService.findAvailableSchedules(today.plusDays(1), null);
        model.addAttribute("availableSchedules", availableSchedules);
        return "find_appointment"; // Имя HTML шаблона для отображения слотов
    }


    // --- Показ формы бронирования для выбранного слота ---
    @GetMapping("/book/{scheduleId}")
    public String showBookingForm(@PathVariable Long scheduleId, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing booking form for scheduleId: {}", scheduleId);
        try {
            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new com.clinic.management.exception.ResourceNotFoundException("Schedule slot not found"));

            if (!schedule.isAvailable()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Извините, этот слот уже занят.");
                return "redirect:/appointments/find"; // Вернуть к поиску
            }

            // !!! ЗАГЛУШКА: Получение ID текущего владельца !!!
            // В реальной системе здесь нужно получить ID владельца из сессии/Spring Security
            Long currentOwnerId = 1L; // ПРЕДПОЛАГАЕМ, ЧТО ВЛАДЕЛЕЦ С ID=1 ЗАЛОГИНЕН

            model.addAttribute("schedule", schedule); // Передаем выбранный слот
            model.addAttribute("appointmentRequest", new AppointmentRequest()); // DTO для формы

            return "book_appointment_form"; // Имя HTML шаблона формы

        } catch (com.clinic.management.exception.ResourceNotFoundException e) {
            log.warn("Error showing booking form: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/appointments/find";
        }
    }

    // --- Обработка данных формы бронирования ---
    @PostMapping("/book")
    public String processBooking(@ModelAttribute AppointmentRequest appointmentRequest, // Используем DTO
                                 RedirectAttributes redirectAttributes) {

        log.info("Processing booking request: {}", appointmentRequest);
        try {
            appointmentService.bookAppointment(
                    appointmentRequest.getScheduleId(),
                    appointmentRequest.getReasonForVisit()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Вы успешно записаны на прием!");
            // Перенаправить на страницу "Мои записи" или дашборд
            return "redirect:/appointments/my"; // Предполагаемый URL для просмотра записей
        } catch (IllegalStateException | com.clinic.management.exception.ResourceNotFoundException e) {
            log.warn("Booking failed: {}", e.getMessage());
            // Возвращаем пользователя обратно к форме бронирования с ошибкой
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка записи: " + e.getMessage());
            // Нужно снова передать ID слота, чтобы вернуться к правильной форме
            if (appointmentRequest.getScheduleId() != null) {
                return "redirect:/appointments/book/" + appointmentRequest.getScheduleId();
            } else {
                return "redirect:/appointments/find"; // Если ID слота потерялся
            }
        } catch (Exception e) {
            log.error("Unexpected error during booking: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Произошла непредвиденная ошибка при записи.");
            return "redirect:/appointments/find";
        }
    }

    // --- Страница просмотра записей (заглушка) ---
    @GetMapping("/my")
    public String showMyAppointments(Model model) {
        // !!! ЗАГЛУШКА: Получение ID текущего владельца !!!
        Long currentOwnerId = 1L;
        // TODO: Получить все записи для всех питомцев владельца
        // List<Appointment> appointments = appointmentService.getAppointmentsForOwner(currentOwnerId);

        return "my_appointments"; // Имя HTML шаблона для просмотра записей
    }

    // DTO для передачи данных из формы бронирования
    // (можно создать как вложенный класс или отдельный файл)
    public static class AppointmentRequest {
        private Long scheduleId;
        private Long petId;
        private String reasonForVisit;
        // геттеры и сеттеры
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }
        public String getReasonForVisit() { return reasonForVisit; }
        public void setReasonForVisit(String reasonForVisit) { this.reasonForVisit = reasonForVisit; }

        @Override
        public String toString() {
            return "AppointmentRequest{" +
                    "scheduleId=" + scheduleId +
                    ", petId=" + petId +
                    ", reasonForVisit='" + reasonForVisit + '\'' +
                    '}';
        }
    }
}