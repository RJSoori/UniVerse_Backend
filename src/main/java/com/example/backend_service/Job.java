package com.example.backend_service;

import jakarta.persistence.*;

@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String externalApplicationUrl; // Redirects students outside

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private Recruiter recruiter;

    public Job() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExternalApplicationUrl() { return externalApplicationUrl; }
    public void setExternalApplicationUrl(String externalApplicationUrl) { this.externalApplicationUrl = externalApplicationUrl; }
    public Recruiter getRecruiter() { return recruiter; }
    public void setRecruiter(Recruiter recruiter) { this.recruiter = recruiter; }
}
