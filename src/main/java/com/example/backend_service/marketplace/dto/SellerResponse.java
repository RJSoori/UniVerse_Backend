package com.example.backend_service.marketplace.dto;

/**
 * Data transfer object for seller information returned in API responses.
 * Excludes sensitive information like password. Used for profile display and marketplace item attribution.
 */
public class SellerResponse {

    private Long id;
    private String storeName;
    private String email;
    private String phone;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}