package com.clinic.management.service;

import com.clinic.management.entity.Medication;
import com.clinic.management.entity.MedicationOrder; // Добавлен импорт
import com.clinic.management.exception.ResourceNotFoundException;
import com.clinic.management.repository.MedicationRepository;
import com.clinic.management.repository.MedicationOrderRepository; // Добавлен импорт
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // Добавлен импорт
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MedicationService {

    private static final Logger log = LoggerFactory.getLogger(MedicationService.class);

    // Статус для нового заказа
    private static final String ORDER_STATUS_PENDING = "PENDING";

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired // Внедряем репозиторий для заказов
    private MedicationOrderRepository medicationOrderRepository;

    // Получить все лекарства (для основной таблицы)
    public List<Medication> getAllMedications() {
        log.debug("Fetching all medications");
        return medicationRepository.findAll();
    }

    // Получить лекарства с низким запасом (для предупреждения и автозаказа)
    public List<Medication> getLowStockMedications() {
        log.debug("Fetching low stock medications");
        List<Medication> allMeds = medicationRepository.findAll();
        List<Medication> lowStock = allMeds.stream()
                .filter(med -> med.getQuantity() < med.getMinStockLevel())
                .collect(Collectors.toList());
        log.debug("Found {} low stock medications", lowStock.size());
        return lowStock;
    }

    // Обновить запас лекарства (для кнопки "Изменить запас")
    public Medication updateMedicationStock(Long id, int quantityChange) {
        log.debug("Updating stock for medication id={}, change={}", id, quantityChange);
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found with id " + id));

        int newQuantity = medication.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            log.warn("Attempted to set negative stock for medication id={}. Setting to 0.", id);
            newQuantity = 0;
        }
        medication.setQuantity(newQuantity);
        Medication savedMedication = medicationRepository.save(medication);
        log.info("Updated stock for medication id={}. New quantity: {}", id, savedMedication.getQuantity());
        return savedMedication;
    }

    // Сохранить новое лекарство или обновить существующее (для формы добавления)
    public Medication saveMedication(Medication medication) {
        log.debug("Saving medication: {}", medication.getName());
        Medication savedMedication = medicationRepository.save(medication);
        log.info("Saved medication id={}, name={}", savedMedication.getId(), savedMedication.getName());
        return savedMedication;
    }

    // Удалить лекарство (для кнопки "Удалить")
    public void deleteMedication(Long id) {
        log.debug("Deleting medication id={}", id);
        if (!medicationRepository.existsById(id)) {
            log.warn("Medication with id={} not found for deletion.", id);
            throw new ResourceNotFoundException("Medication not found with id " + id);
        }
        medicationRepository.deleteById(id);
        log.info("Deleted medication id={}", id);
    }

    /**
     * Запланированный метод для автоматической проверки и заказа лекарств.
     * Запускается по расписанию (например, раз в час).
     * Если количество лекарства меньше минимального запаса, создает
     * запись в таблице MedicationOrder и увеличивает количество лекарства
     * на (minStockLevel * 2 - currentQuantity), чтобы довести запас
     * до двойного минимального уровня.
     */
    @Scheduled(cron = "0 */1 * * * ?") // Пример: Запускать каждый час
    // @Scheduled(cron = "0 */1 * * * ?") // Для теста: Запускать каждую минуту
    public void checkStockLevelsAndOrder() {
        log.info("Running scheduled stock check and auto-order...");
        List<Medication> lowStockMedications = getLowStockMedications();

        if (lowStockMedications.isEmpty()) {
            log.info("Scheduled stock check: No low stock medications found. No orders needed.");
            return;
        }

        log.warn("Scheduled stock check found {} low stock medication(s). Creating automatic orders...", lowStockMedications.size());

        for (Medication med : lowStockMedications) {
            try {
                int currentQuantity = med.getQuantity();
                int minStock = med.getMinStockLevel();

                // Рассчитываем количество для заказа:
                // нужно (minStock - currentQuantity) чтобы достичь минимума
                // + еще minStock (минимальный запас)
                int quantityToOrder = (minStock - currentQuantity) + minStock;
                // Или проще: (minStock * 2) - currentQuantity

                // Убедимся, что заказываем положительное количество
                if (quantityToOrder <= 0) {
                    log.warn("Skipping order for {} (ID: {}): Calculated order quantity ({}) is not positive. Current: {}, Min: {}",
                            med.getName(), med.getId(), quantityToOrder, currentQuantity, minStock);
                    continue; // Переходим к следующему лекарству
                }

                log.info("AUTO-ORDER: Creating order for Medication ID: {}, Name: '{}', Current Qty: {}, Min Stock: {}, Quantity to Order: {}",
                        med.getId(), med.getName(), currentQuantity, minStock, quantityToOrder);

                // 1. Создаем и сохраняем запись о заказе
                MedicationOrder newOrder = new MedicationOrder();
                newOrder.setMedication(med);
                newOrder.setQuantityOrdered(quantityToOrder);
                newOrder.setOrderDate(LocalDateTime.now());
                newOrder.setStatus(ORDER_STATUS_PENDING); // Используем константу
                medicationOrderRepository.save(newOrder);
                log.info("AUTO-ORDER: Saved new MedicationOrder with ID: {} for Medication '{}'", newOrder.getId(), med.getName());

                // 2. Увеличиваем текущее количество лекарства
                // ВАЖНО: Это имитация немедленного пополнения. В реальной системе
                // запас должен увеличиваться только ПОСЛЕ фактического получения заказа.
                int updatedQuantity = currentQuantity + quantityToOrder;
                med.setQuantity(updatedQuantity);
                medicationRepository.save(med); // Сохраняем обновленное лекарство
                log.info("AUTO-ORDER: Updated stock for Medication ID: {} ('{}') to new quantity: {}", med.getId(), med.getName(), updatedQuantity);

            } catch (Exception e) {
                // Логируем ошибку для конкретного лекарства и продолжаем проверку остальных
                log.error("AUTO-ORDER: Failed to process automatic order for medication ID: {}, Name: '{}'. Error: {}",
                        med.getId(), med.getName() != null ? med.getName() : "N/A", e.getMessage(), e);
                // Здесь можно добавить механизм уведомления администратора об ошибке
            }
        }
        log.info("Scheduled stock check and auto-order finished.");
    }
}