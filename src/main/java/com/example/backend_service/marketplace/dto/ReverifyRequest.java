package com.example.backend_service.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

public record ReverifyRequest(@NotBlank String reason) {
}
