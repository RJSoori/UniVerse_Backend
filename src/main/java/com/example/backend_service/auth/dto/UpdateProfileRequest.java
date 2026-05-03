package com.example.backend_service.auth.dto;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(String name, @Email String email, String degree) {
}
