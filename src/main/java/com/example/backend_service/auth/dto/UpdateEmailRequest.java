package com.example.backend_service.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank @Email String email,
        @NotBlank String emailVerificationToken) {
}
