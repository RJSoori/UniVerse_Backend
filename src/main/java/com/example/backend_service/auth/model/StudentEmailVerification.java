package com.example.backend_service.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

/**
 * Tracks email verification codes issued during student signup, before a Student account
 * exists yet. Keyed by the (lowercased) email address being verified — one pending
 * verification per email, overwritten each time a new code is requested. Mirrors
 * {@code jobhub.model.RecruiterEmailVerification}.
 */
@Entity
public class StudentEmailVerification {

    @Id
    private String email;

    @JsonIgnore
    private String codeHash;
    @JsonIgnore
    private Instant codeExpiresAt;
    @JsonIgnore
    private int codeAttempts = 0;

    @JsonIgnore
    private String verificationTokenHash;
    @JsonIgnore
    private Instant verificationTokenExpiresAt;

    public StudentEmailVerification() {
    }

    public StudentEmailVerification(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Instant getCodeExpiresAt() { return codeExpiresAt; }
    public void setCodeExpiresAt(Instant codeExpiresAt) { this.codeExpiresAt = codeExpiresAt; }
    public int getCodeAttempts() { return codeAttempts; }
    public void setCodeAttempts(int codeAttempts) { this.codeAttempts = codeAttempts; }
    public String getVerificationTokenHash() { return verificationTokenHash; }
    public void setVerificationTokenHash(String verificationTokenHash) { this.verificationTokenHash = verificationTokenHash; }
    public Instant getVerificationTokenExpiresAt() { return verificationTokenExpiresAt; }
    public void setVerificationTokenExpiresAt(Instant verificationTokenExpiresAt) { this.verificationTokenExpiresAt = verificationTokenExpiresAt; }
}
