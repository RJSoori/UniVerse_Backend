package com.example.backend_service.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for seller registration requests.
 * Contains all required fields for creating a new seller account with validation constraints.
 */
public class SellerRequest {

    @NotBlank
    private String storeName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phone;

    private String description;

    // Populated by the controller after uploading the corresponding multipart file, if any.
    private String identityDocumentUrl;
    private String shopLogoUrl;
    private String proofOfItemsUrl;

    @NotBlank
    private String emailVerificationToken;

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIdentityDocumentUrl() { return identityDocumentUrl; }
    public void setIdentityDocumentUrl(String identityDocumentUrl) { this.identityDocumentUrl = identityDocumentUrl; }
    public String getShopLogoUrl() { return shopLogoUrl; }
    public void setShopLogoUrl(String shopLogoUrl) { this.shopLogoUrl = shopLogoUrl; }
    public String getProofOfItemsUrl() { return proofOfItemsUrl; }
    public void setProofOfItemsUrl(String proofOfItemsUrl) { this.proofOfItemsUrl = proofOfItemsUrl; }
    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }
}