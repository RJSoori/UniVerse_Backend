package com.example.backend_service.skills;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.Instant;

/**
 * One row per student: the skills shown on their Job Hub profile, plus a record of the
 * most recently uploaded CV (kept in Azure Blob Storage — see {@link com.example.backend_service.AzureBlobService})
 * used to auto-populate that list via Gemini extraction.
 */
@Entity
public class StudentSkillProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long studentId;

    /** JSON array of skill name strings, e.g. ["React","SQL","Project Management"]. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String skillsJson = "[]";

    private String cvUrl;

    private Instant cvUploadedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getSkillsJson() { return skillsJson; }
    public void setSkillsJson(String skillsJson) { this.skillsJson = skillsJson; }
    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }
    public Instant getCvUploadedAt() { return cvUploadedAt; }
    public void setCvUploadedAt(Instant cvUploadedAt) { this.cvUploadedAt = cvUploadedAt; }
}
