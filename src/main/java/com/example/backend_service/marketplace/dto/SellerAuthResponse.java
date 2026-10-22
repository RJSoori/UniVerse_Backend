package com.example.backend_service.marketplace.dto;

/**
 * Data transfer object for seller authentication responses.
 * Returned after successful login or registration with JWT token and seller profile information.
 */
public class SellerAuthResponse {

    private String token;
    private SellerResponse seller;

    public SellerAuthResponse(String token, SellerResponse seller) {
        this.token = token;
        this.seller = seller;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public SellerResponse getSeller() { return seller; }
    public void setSeller(SellerResponse seller) { this.seller = seller; }
}