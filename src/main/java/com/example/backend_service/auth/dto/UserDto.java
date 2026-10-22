package com.example.backend_service.auth.dto;

import com.example.backend_service.Role;
import com.example.backend_service.Student;
import java.time.LocalDateTime;

/**
 * Lightweight DTO sent to clients. Includes `createdAt` so frontend can
 * apply onboarding logic (first-month suggestions) without additional calls.
 */
public record UserDto(Long id, String username, String name, String email, String degree, String profilePictureUrl, Role role, LocalDateTime createdAt) {
    public static UserDto from(Student s) {
        return new UserDto(s.getId(), s.getUsername(), s.getName(), s.getEmail(), s.getDegree(), s.getProfilePictureUrl(), s.getRole(), s.getCreatedAt());
    }
}
