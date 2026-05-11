package com.example.backend_service.jobhub.model;

import com.example.backend_service.RecruiterStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    @JsonIgnore
    private String password;
    private String businessRegistrationUrl;
    private String orgLogoUrl;
    private String authLetterUrl;
    private String accountType = "company";

    @Enumerated(EnumType.STRING)
    private RecruiterStatus status = RecruiterStatus.PENDING;

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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getBusinessRegistrationUrl() { return businessRegistrationUrl; }
    public void setBusinessRegistrationUrl(String businessRegistrationUrl) { this.businessRegistrationUrl = businessRegistrationUrl; }
    public String getOrgLogoUrl() { return orgLogoUrl; }
    public void setOrgLogoUrl(String orgLogoUrl) { this.orgLogoUrl = orgLogoUrl; }
    public String getAuthLetterUrl() { return authLetterUrl; }
    public void setAuthLetterUrl(String authLetterUrl) { this.authLetterUrl = authLetterUrl; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public RecruiterStatus getStatus() { return status; }
    public void setStatus(RecruiterStatus status) { this.status = status; }
}