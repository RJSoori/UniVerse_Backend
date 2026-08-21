package com.example.backend_service.jobhub.dto;

/**
 * A recruiter's own profile, for the self-service "Portal Settings" page - unlike
 * {@link RecruiterAuthResponse} (returned only at login/register), this includes the
 * verification document URLs and carries no JWT.
 */
public class RecruiterProfileResponse {

    private Long id;
    private String companyName;
    private String email;
    private String contactPerson;
    private String accountType;
    private String status;
    private String businessRegistrationUrl;
    private String orgLogoUrl;
    private String authLetterUrl;
    private String profilePictureUrl;
    private String idDocumentUrl;

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
    public String getBusinessRegistrationUrl() { return businessRegistrationUrl; }
    public void setBusinessRegistrationUrl(String businessRegistrationUrl) { this.businessRegistrationUrl = businessRegistrationUrl; }
    public String getOrgLogoUrl() { return orgLogoUrl; }
    public void setOrgLogoUrl(String orgLogoUrl) { this.orgLogoUrl = orgLogoUrl; }
    public String getAuthLetterUrl() { return authLetterUrl; }
    public void setAuthLetterUrl(String authLetterUrl) { this.authLetterUrl = authLetterUrl; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    public String getIdDocumentUrl() { return idDocumentUrl; }
    public void setIdDocumentUrl(String idDocumentUrl) { this.idDocumentUrl = idDocumentUrl; }
}
