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

    /**
     * Null until the student explicitly accepts the notice that their CV is sent to Google's
     * Gemini API for skill extraction. Uploads are hard-blocked server-side until this is set -
     * see SkillsController#uploadCv - so the consent can never be skipped by calling the API
     * directly, and once accepted it's never asked for again.
     */
    private Instant cvPrivacyPolicyAcceptedAt;

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
    public Instant getCvPrivacyPolicyAcceptedAt() { return cvPrivacyPolicyAcceptedAt; }
    public void setCvPrivacyPolicyAcceptedAt(Instant cvPrivacyPolicyAcceptedAt) { this.cvPrivacyPolicyAcceptedAt = cvPrivacyPolicyAcceptedAt; }
}
