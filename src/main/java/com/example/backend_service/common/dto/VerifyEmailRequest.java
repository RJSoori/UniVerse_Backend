package com.example.backend_service.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VerifyEmailRequest(String email, String code) {
}
