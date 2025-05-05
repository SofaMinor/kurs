package com.clinic.management.service;

import com.clinic.management.entity.*; // Импортируем все нужные сущности
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.*; // Импортируем все нужные репозитории
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final String STATUS_BOOKED = "BOOKED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository; // Может понадобиться для поиска по доктору

    /**
     * Находит доступные слоты в расписании на указанную дату для конкретного врача (если указан).
     * @param date Дата
     * @param doctorId ID врача (опционально)
     * @return Список доступных Schedule
     */
    public List<Schedule> findAvailableSchedules(LocalDate date, Long doctorId) {
        log.debug("Finding available schedules for date: {}, doctorId: {}", date, doctorId);
        List<Schedule> schedules;
        if (doctorId != null) {
            // TODO: Проверить, существует ли доктор с таким ID перед вызовом репозитория
            // doctorRepository.findById(doctorId).orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
            schedules = scheduleRepository.findByDoctorId(doctorId);
        } else {
            schedules = scheduleRepository.findAll();
        }

        // Фильтруем по дате и доступности
        return schedules.stream()
                .filter(s -> s.getStartTime() != null && s.getStartTime().toLocalDate().equals(date)) // Добавлена проверка на null
                .filter(Schedule::isAvailable) // Только доступные слоты
                .collect(Collectors.toList());
        // TODO: Можно добавить поиск по специализации врача, если нужно
    }


    /**
     * Создает запись на прием для питомца к врачу в выбранный слот расписания.
     * @param scheduleId ID слота расписания
     * @param reasonForVisit Причина визита
     * @return Созданный Appointment
     * @throws ResourceNotFoundException если слот или питомец не найдены
     * @throws IllegalStateException если слот уже занят или информация о докторе отсутствует
     */
    @Transactional // Важно для консистентности данных
    public Appointment bookAppointment(Long scheduleId, String reasonForVisit) {
        log.info("Attempting to book appointment for scheduleId: {}", scheduleId);

        // 1. Найти слот расписания и проверить доступность
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule slot not found with id: " + scheduleId));

        if (!schedule.isAvailable()) {
            log.warn("Attempted to book an already unavailable schedule slot: {}", scheduleId);
            throw new IllegalStateException("Selected time slot is no longer available.");
        }

        // 2. TODO: Найти питомца (Pet) по его ID (petId) - подразумевается, что petId будет добавлен как параметр
        // Pet pet = petRepository.findById(petId)
        //         .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + petId));
        // Пока что убрали зависимость от Pet, как в оригинальном коде


        // 3. Получить доктора из расписания
        Doctor doctor = schedule.getDoctor();
        if (doctor == null) {
            // Этого не должно быть, если данные консистентны, но проверим
            log.error("Doctor is null for scheduleId: {}", scheduleId);
            throw new IllegalStateException("Doctor information is missing for the selected schedule.");
        }

        // 4. Создать новую запись
        Appointment appointment = new Appointment();
        appointment.setSchedule(schedule);
        // appointment.setPet(pet); // TODO: Установить питомца, когда он будет добавлен
        appointment.setDoctor(doctor);
        appointment.setAppointmentTime(schedule.getStartTime()); // Время начала из слота
        appointment.setReasonForVisit(reasonForVisit);
        appointment.setStatus(STATUS_BOOKED);
        appointment.setCreatedAt(LocalDateTime.now());

        // 5. Пометить слот как занятый
        schedule.setAvailable(false);
        scheduleRepository.save(schedule); // Сохраняем измененный слот

        // 6. Сохранить новую запись
        Appointment savedAppointment = appointmentRepository.save(appointment);

        log.info("Successfully booked appointment with ID: {} at schedule slot ID: {}",
                savedAppointment.getId(), scheduleId); // Убрали Pet ID из лога

        return savedAppointment;
    }

    /**
     * Отменяет запись на прием.
     * @param appointmentId ID записи для отмены
     * @throws ResourceNotFoundException если запись не найдена
     * @throws IllegalStateException если запись уже отменена или не в статусе BOOKED
     */
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        log.info("Attempting to cancel appointment with ID: {}", appointmentId);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        if (!STATUS_BOOKED.equals(appointment.getStatus())) {
            log.warn("Attempted to cancel an appointment that is not in BOOKED state (ID: {}, Status: {})",
                    appointmentId, appointment.getStatus());
            throw new IllegalStateException("Appointment cannot be cancelled as it's not in BOOKED state.");
        }

        // Освобождаем слот расписания
        Schedule schedule = appointment.getSchedule();
        if (schedule != null) {
            // Проверяем, не занят ли слот уже другой записью (маловероятно при правильной логике, но безопасно)
            // В данном простом случае просто делаем доступным
            schedule.setAvailable(true);
            scheduleRepository.save(schedule);
            log.info("Made schedule slot ID: {} available again.", schedule.getId());
        } else {
            // Это может произойти, если связь appointment -> schedule была нарушена или не установлена
            log.warn("Cannot make schedule available for cancelled appointment ID: {} because schedule link is missing.", appointmentId);
        }

        // Обновляем статус записи
        appointment.setStatus(STATUS_CANCELLED);
        // appointment.setUpdatedAt(LocalDateTime.now()); // Можно добавить поле для отслеживания времени изменения
        appointmentRepository.save(appointment);
        log.info("Successfully cancelled appointment with ID: {}", appointmentId);
    }


    /**
     * Получает список записей для конкретного доктора.
     * @param doctorId ID доктора
     * @return Список записей
     */
    public List<Appointment> getAppointmentsForDoctor(Long doctorId) {
        log.debug("Fetching appointments for doctorId: {}", doctorId);
        // TODO: Желательно проверить существование доктора перед запросом
        // doctorRepository.findById(doctorId).orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
        return appointmentRepository.findByDoctorId(doctorId);
    }

    // TODO: Добавить метод getAppointmentsForOwner(Long ownerId)
    // Это потребует либо связи Owner->Pet->Appointment, либо денормализации ownerId в Appointment

    /**
     * Возвращает общее количество всех записей на прием в системе.
     * Использует стандартный метод count() из Spring Data JPA репозитория.
     * @return Общее количество записей (long).
     */
    public long countAllAppointments() {
        log.debug("Requesting total count of all appointments.");
        long count = appointmentRepository.count(); // Метод count() предоставляется CrudRepository/JpaRepository
        log.info("Total number of appointments in the system: {}", count);
        return count;
    }

    /**
     * Находит запись по ID.
     * @param appointmentId ID записи
     * @return Найденная запись
     * @throws ResourceNotFoundException если запись не найдена
     */
    public Appointment findAppointmentById(Long appointmentId) {
        log.debug("Fetching appointment by ID: {}", appointmentId);
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
    }

    /**
     * Получает все записи на прием.
     * ОСТОРОЖНО: Может вернуть большой объем данных. Рассмотрите пагинацию для реальных приложений.
     * @return Список всех записей.
     */
    public List<Appointment> findAllAppointments() {
        log.debug("Fetching all appointments.");
        List<Appointment> appointments = appointmentRepository.findAll();
        log.info("Found {} total appointments.", appointments.size());
        // В реальном приложении здесь нужна пагинация:
        // public Page<Appointment> findAllAppointments(Pageable pageable) {
        //    return appointmentRepository.findAll(pageable);
        // }
        return appointments;
    }

}