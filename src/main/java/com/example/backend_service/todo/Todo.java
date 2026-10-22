package com.example.backend_service.todo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity for a single to-do item.
 * Maps directly to the 'todos' table in the database.
 */
@Entity
@Table(name = "todos")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "title", nullable = false, columnDefinition = "VARCHAR(255)")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date", columnDefinition = "DATE")
    private LocalDate dueDate;

    @Column(name = "due_time", columnDefinition = "TIME")
    private String dueTime;

    @Column(name = "duration_minutes", columnDefinition = "INT")
    private String durationMinutes;

    @Column(name = "priority", columnDefinition = "VARCHAR(20)")
    private String priority;

    @Column(name = "completed", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean completed = false;

    @Column(name = "reminder_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean reminderEnabled = true;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Todo() {}

    public Todo(Long studentId, String title, String description, LocalDate dueDate, String dueTime, 
                String durationMinutes, String priority, Boolean completed, Boolean reminderEnabled) {
        this.studentId = studentId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.durationMinutes = durationMinutes;
        this.priority = priority;
        this.completed = completed;
        this.reminderEnabled = reminderEnabled;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }

    public String getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(String durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public Boolean getReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(Boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
