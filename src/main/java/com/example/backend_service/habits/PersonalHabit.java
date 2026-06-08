package com.example.backend_service.habits;

import jakarta.persistence.*;

/**
 * Database entity for habit data.
 */
/**
 * Represents a habit that one student tracks.
 * Stores habit name, description, category, and completion dates.
 */
@Entity
@Table(name = "personal_habits")
public class PersonalHabit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    private String name;
    private String description;
    private String category;
    private String color;

    @Column(name = "focus_area")
    private String focusArea;

    @Column(name = "icon_id")
    private String iconId;

    @Lob
    @Column(name = "completed_dates_json", columnDefinition = "LONGTEXT")
    private String completedDatesJson;

    public PersonalHabit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getFocusArea() { return focusArea; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }

    public String getIconId() { return iconId; }
    public void setIconId(String iconId) { this.iconId = iconId; }

    /**
     * Handles dates stored as a JSON string.
     */
    public String getCompletedDatesJson() { return completedDatesJson; }
    public void setCompletedDatesJson(String completedDatesJson) { this.completedDatesJson = completedDatesJson; }
}