package com.example.backend_service.schedule;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single event in a student's calendar.
 * Stores event details like title, date, time, and description.
 */
@Entity
@Table(name = "schedule_event")
public class ScheduleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private String title;

    @Column(columnDefinition = "DATE")
    private LocalDate date;

    private String startTime;

    private String endTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Client-facing category, e.g. "class", "study", "meeting", "other". */
    private String type;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ScheduleEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}