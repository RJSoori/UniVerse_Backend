package com.example.backend_service.marketplace.model;

import com.example.backend_service.marketplace.enums.SellerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Represents a seller account in the marketplace.
 * Stores seller's store information, contact details, and encrypted password.
 * Each seller can list multiple marketplace items for sale or rent.
 */
@Entity
@Table(name = "sellers")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String storeName;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    private String password;

    private String phone;

    private String description;

    @Enumerated(EnumType.STRING)
    private SellerStatus status = SellerStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    public Seller() {}

    @PrePersist
    private void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public SellerStatus getStatus() { return status; }
    public void setStatus(SellerStatus status) { this.status = status; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}