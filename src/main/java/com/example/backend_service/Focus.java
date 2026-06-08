package com.example.backend_service;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "focus_sessions") 
public class Focus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "focus_date") 
    private LocalDate focusDate;

    @Column(name = "total_minutes") 
    private Integer totalMinutes;

    // Default Constructor
    public Focus() {}

    // Getters and Setters
    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getFocusDate() { 
        return focusDate; 
    }
    public void setFocusDate(LocalDate focusDate) { 
        this.focusDate = focusDate; 
    }

    public Integer getTotalMinutes() { 
        return totalMinutes; 
    }
    public void setTotalMinutes(Integer totalMinutes) { 
        this.totalMinutes = totalMinutes; 
    }
}