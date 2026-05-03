package com.example.backend_service.auth.dto;

import com.example.backend_service.Role;
import com.example.backend_service.Student;

public record UserDto(Long id, String username, String name, String email, String degree, Role role) {
    public static UserDto from(Student s) {
        return new UserDto(s.getId(), s.getUsername(), s.getName(), s.getEmail(), s.getDegree(), s.getRole());
    }
}
