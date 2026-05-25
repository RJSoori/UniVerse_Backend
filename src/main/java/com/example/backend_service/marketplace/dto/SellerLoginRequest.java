package com.example.backend_service.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for seller login requests.
 * Contains credentials (username and password) needed to authenticate a seller.
 */
public class SellerLoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}