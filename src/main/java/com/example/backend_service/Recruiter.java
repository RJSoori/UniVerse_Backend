package com.example.backend_service;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Recruiter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;
    private String email;
    private String contactPerson;

    // Document storage fields (URLs to stored documents)
    private String businessRegistrationUrl;
    private String orgLogoUrl;
    private String authLetterUrl;

    // Verification status
    @Enumerated(EnumType.STRING)
    private VerificationStatus status = VerificationStatus.PENDING;

    private LocalDateTime registeredAt = LocalDateTime.now();

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED
    }

    public Recruiter() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getBusinessRegistrationUrl() { return businessRegistrationUrl; }
    public void setBusinessRegistrationUrl(String businessRegistrationUrl) { this.businessRegistrationUrl = businessRegistrationUrl; }
    public String getOrgLogoUrl() { return orgLogoUrl; }
    public void setOrgLogoUrl(String orgLogoUrl) { this.orgLogoUrl = orgLogoUrl; }
    public String getAuthLetterUrl() { return authLetterUrl; }
    public void setAuthLetterUrl(String authLetterUrl) { this.authLetterUrl = authLetterUrl; }
    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}