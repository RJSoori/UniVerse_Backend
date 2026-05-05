package com.example.backend_service.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SellerRequest {

    @NotBlank
    private String storeName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    private String description;

    // Getters and Setters
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}