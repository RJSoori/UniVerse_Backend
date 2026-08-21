package com.example.backend_service.jobhub.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

/**
 * A student's report against a job posting. A posting can accumulate several reports before
 * an admin acts on it - {@link #resolved} flips to true for every open report on the job the
 * moment an admin dismisses the report or blocks the posting (see JobHubController), so
 * "currently under investigation" is always just "any unresolved report exists for this job".
 */
@Entity
public class JobReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    private Long reportedByStudentId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String reason;

    private Instant reportedAt;

    private boolean resolved = false;

    public JobReport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public Long getReportedByStudentId() { return reportedByStudentId; }
    public void setReportedByStudentId(Long reportedByStudentId) { this.reportedByStudentId = reportedByStudentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
