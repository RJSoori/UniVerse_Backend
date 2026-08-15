package com.example.backend_service;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "students", uniqueConstraints = {
        @UniqueConstraint(name = "uk_students_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_students_email", columnNames = "email")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String degree;

    private String profilePictureUrl;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String username;

    @JsonIgnore
    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.STUDENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    /**
     * Server-side account creation timestamp. Exposed to clients to support
     * onboarding UX (e.g. first-month habit suggestions) and sorting.
     */
    private LocalDateTime createdAt;

    // ── Password reset (forgot password flow) ──
    @JsonIgnore
    private String resetCodeHash;
    @JsonIgnore
    private java.time.Instant resetCodeExpiresAt;
    @JsonIgnore
    private int resetCodeAttempts = 0;
    @JsonIgnore
    private String resetTokenHash;
    @JsonIgnore
    private java.time.Instant resetTokenExpiresAt;

    public Student() {}

    public Student(String name, String degree, String email, String username, String password) {
        this.name = name;
        this.degree = degree;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getResetCodeHash() { return resetCodeHash; }
    public void setResetCodeHash(String resetCodeHash) { this.resetCodeHash = resetCodeHash; }
    public java.time.Instant getResetCodeExpiresAt() { return resetCodeExpiresAt; }
    public void setResetCodeExpiresAt(java.time.Instant resetCodeExpiresAt) { this.resetCodeExpiresAt = resetCodeExpiresAt; }
    public int getResetCodeAttempts() { return resetCodeAttempts; }
    public void setResetCodeAttempts(int resetCodeAttempts) { this.resetCodeAttempts = resetCodeAttempts; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String resetTokenHash) { this.resetTokenHash = resetTokenHash; }
    public java.time.Instant getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(java.time.Instant resetTokenExpiresAt) { this.resetTokenExpiresAt = resetTokenExpiresAt; }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
