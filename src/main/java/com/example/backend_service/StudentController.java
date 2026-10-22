package com.example.backend_service;

import com.example.backend_service.auth.dto.UserDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Auth has moved to {@link com.example.backend_service.auth.AuthController} —
 * legacy /login and /register endpoints have been removed.
 *
 * The remaining listing endpoint is admin-gated and CORS is governed centrally
 * by {@link com.example.backend_service.security.CorsConfig}.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // Admin-only endpoint to list all students, used for admin management UI
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllStudents() {
        return studentRepository.findAll().stream().map(UserDto::from).toList();
    }
}
