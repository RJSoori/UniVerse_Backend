package com.example.backend_service.jobhub.model;

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

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    /**
     * Recruiter-controlled visibility, independent of the admin-controlled {@link #status}.
     * A job only shows up on the student side ({@code GET /api/jobs/all}) when it is both
     * APPROVED and active - the recruiter can deactivate/reactivate it anytime without losing
     * the posting or needing re-approval.
     */
    private boolean active = true;

    /**
     * Soft-delete flag. "Deleting" a posting never removes the row - the title and skills
     * keep contributing to the UniVerse Skill Matcher's suggested-skills signal even after the
     * recruiter deletes it, per product decision. Deleting also forces {@link #active} false,
     * so it's excluded from both the recruiter's own dashboard list and student browsing.
     */
    private boolean deleted = false;

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
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Recruiter getRecruiter() { return recruiter; }
    public void setRecruiter(Recruiter recruiter) { this.recruiter = recruiter; }
}
