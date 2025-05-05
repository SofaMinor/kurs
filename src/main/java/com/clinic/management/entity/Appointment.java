package com.clinic.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "appointments") // Новая таблица
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Какая запись (слот) в расписании занята
    @OneToOne // Одна запись занимает один слот
    @JoinColumn(name = "schedule_id", unique = true) // Слот может быть занят только одной записью
    private Schedule schedule; // Используем существующее расписание

    // Какой ветеринар ведет прием (дублирует информацию из Schedule, но может быть полезно)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDateTime appointmentTime; // Точное время начала (из Schedule)

    @Column(length = 500)
    private String reasonForVisit; // Причина обращения

    @Column(nullable = false)
    private String status; // Например: BOOKED, COMPLETED, CANCELLED

    private LocalDateTime createdAt; // Когда создана запись
}