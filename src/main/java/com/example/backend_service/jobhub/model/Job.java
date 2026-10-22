package com.example.backend_service.jobhub.model;

import java.time.Instant;

import com.example.backend_service.jobhub.enums.JobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

@Entity
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Free-text fields backed by multi-line Textareas in the posting form - the default
    // VARCHAR(255) silently rejected any real-world description/requirements/skills list
    // longer than 255 characters (MysqlDataTruncation), so posting failed for most genuine
    // job descriptions.
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String skills;

    private String salaryInfo;
    private String workType;
    private String employmentType;
    private String postedAt;
    private String externalApplicationUrl;

    
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    
    private boolean active = true;

    
    private boolean deleted = false;

    
    private boolean underReview = false;

    
    private boolean blocked = false;

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
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getSalaryInfo() { return salaryInfo; }
    public void setSalaryInfo(String salaryInfo) { this.salaryInfo = salaryInfo; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getPostedAt() { return postedAt; }
    public void setPostedAt(String postedAt) { this.postedAt = postedAt; }
    public String getExternalApplicationUrl() { return externalApplicationUrl; }
    public void setExternalApplicationUrl(String externalApplicationUrl) { this.externalApplicationUrl = externalApplicationUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public boolean isUnderReview() { return underReview; }
    public void setUnderReview(boolean underReview) { this.underReview = underReview; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public Recruiter getRecruiter() { return recruiter; }
    public void setRecruiter(Recruiter recruiter) { this.recruiter = recruiter; }
}
