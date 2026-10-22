package com.example.backend_service.habits;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data object used for sending and receiving habit info.
 * Maps habit details to JSON for API use.
 */
public class PersonalHabitDto {
    private Long id;
    private String name;
    private String description;
    private String color;
    @JsonProperty("iconId")
    private String iconId;
    private String category; // "build" or "break"
    private String focusArea; // "education", "health", "fitness", etc.
    @JsonProperty("completedDates")
    private List<String> completedDates;
    private String createdAt;

    public PersonalHabitDto() {
    }

    /**
     * Creates a habit DTO with basic details.
     */
    public PersonalHabitDto(String name, String description, String color, String category, String focusArea) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.category = category;
        this.focusArea = focusArea;
        this.completedDates = List.of();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIconId() { return iconId; }
    public void setIconId(String iconId) { this.iconId = iconId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFocusArea() { return focusArea; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }

    public List<String> getCompletedDates() { return completedDates; }
    public void setCompletedDates(List<String> completedDates) { this.completedDates = completedDates; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}