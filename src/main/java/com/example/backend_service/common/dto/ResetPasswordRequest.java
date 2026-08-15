package com.example.backend_service.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResetPasswordRequest(String email, String resetToken, String newPassword) {
}
