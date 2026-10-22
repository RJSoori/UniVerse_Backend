package com.example.backend_service.jobhub.dto;

public class RecruiterAuthResponse {

    private String token;
    private Long id;
    private String companyName;
    private String email;
    private String contactPerson;
    private String accountType;
    private String status;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
