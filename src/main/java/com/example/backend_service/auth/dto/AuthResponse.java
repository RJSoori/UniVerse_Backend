package com.example.backend_service.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
