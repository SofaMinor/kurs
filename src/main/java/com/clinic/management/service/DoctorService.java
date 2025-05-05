package com.clinic.management.service;


import com.clinic.management.entity.Doctor; // Импортируем сущность Doctor
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.DoctorRepository; // Импортируем репозиторий DoctorRepository
// Импортируем репозитории, которые могут понадобиться для проверок связанных сущностей (опционально)
import com.clinic.management.repository.AppointmentRepository;
import com.clinic.management.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional; // Для использования в update

@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    @Autowired
    private DoctorRepository doctorRepository;

    // Опционально: для проверок перед удалением
    @Autowired(required = false) // Делаем необязательными, если не используется проверка
    private AppointmentRepository appointmentRepository;

    @Autowired(required = false) // Делаем необязательными, если не используется проверка
    private ScheduleRepository scheduleRepository;


    /**
     * Находит доктора по его ID.
     * @param doctorId ID доктора
     * @return Найденный объект Doctor
     * @throws ResourceNotFoundException если доктор с таким ID не найден
     */
    public Doctor findDoctorById(Long doctorId) {
        log.debug("Fetching doctor by ID: {}", doctorId);
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
    }

    /**
     * Возвращает список всех докторов в системе.
     * ОСТОРОЖНО: Может вернуть большой объем данных. Рассмотрите пагинацию для реальных приложений.
     * @return Список всех докторов (List<Doctor>).
     */
    public List<Doctor> findAllDoctors() {
        log.debug("Fetching all doctors.");
        List<Doctor> doctors = doctorRepository.findAll();
        log.info("Found {} total doctors.", doctors.size());
        // В реальном приложении здесь нужна пагинация:
        // public Page<Doctor> findAllDoctors(Pageable pageable) {
        //    return doctorRepository.findAll(pageable);
        // }
        return doctors;
    }

    /**
     * Создает нового доктора в системе.
     * @param doctor Объект Doctor с данными для создания. ID должен быть null или отсутствовать.
     * @return Сохраненный объект Doctor с присвоенным ID.
     */


    /**
     * Обновляет информацию о существующем докторе.
     * @param doctorId ID доктора, которого нужно обновить.
     * @param doctorDetails Объект Doctor с новыми данными.
     * @return Обновленный объект Doctor.
     * @throws ResourceNotFoundException если доктор с указанным ID не найден.
     */


    /**
     * Удаляет доктора по ID.
     * ВНИМАНИЕ: Это действие необратимо и может вызвать проблемы, если на доктора ссылаются
     * другие записи (расписания, приемы). Рассмотрите использование "мягкого удаления" (soft delete)
     * или добавление проверок на связанные сущности перед удалением.
     * @param doctorId ID доктора для удаления.
     * @throws ResourceNotFoundException если доктор не найден.
     * @throws IllegalStateException если есть связанные сущности, препятствующие удалению (опциональная проверка).
     */
    @Transactional
    public void deleteDoctor(Long doctorId) {
        log.info("Attempting to delete doctor with ID: {}", doctorId);

        // 1. Проверить, существует ли доктор
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with id: " + doctorId);
        }

        // 2. TODO: Опциональная проверка на связанные сущности
        // Например, проверить, есть ли у доктора предстоящие записи или активные слоты в расписании
        /*
        if (appointmentRepository != null && appointmentRepository.existsByDoctorIdAndStatus(doctorId, "BOOKED")) { // Пример проверки
            log.warn("Cannot delete doctor ID: {} because they have booked appointments.", doctorId);
            throw new IllegalStateException("Cannot delete doctor with active appointments.");
        }
        if (scheduleRepository != null && scheduleRepository.existsByDoctorIdAndStartTimeAfter(doctorId, LocalDateTime.now())) { // Пример проверки
             log.warn("Cannot delete doctor ID: {} because they have future schedule slots.", doctorId);
             throw new IllegalStateException("Cannot delete doctor with future schedule slots.");
        }
        */

        // 3. Удалить доктора
        doctorRepository.deleteById(doctorId);
        log.info("Successfully deleted doctor with ID: {}", doctorId);
    }



    /**
     * Возвращает общее количество докторов в системе.
     * @return Общее количество докторов (long).
     */
    public long countAllDoctors() {
        log.debug("Requesting total count of all doctors.");
        long count = doctorRepository.count();
        log.info("Total number of doctors in the system: {}", count);
        return count;
    }

    // TODO: Можно добавить другие методы по необходимости, например:
    // - findDoctorsByName(String name)
    // - Поиск с пагинацией и сортировкой
    // - Методы для "мягкого" удаления/восстановления (если используется флаг is_active/is_deleted)
}