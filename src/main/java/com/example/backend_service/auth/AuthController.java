package com.example.backend_service.auth;

import com.example.backend_service.Role;
import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.auth.dto.AuthResponse;
import com.example.backend_service.auth.dto.LoginRequest;
import com.example.backend_service.auth.dto.RegisterRequest;
import com.example.backend_service.auth.dto.UpdateProfileRequest;
import com.example.backend_service.auth.dto.UserDto;
import com.example.backend_service.common.exception.ConflictException;
import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (studentRepository.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken");
        }
        if (studentRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered");
        }
        Student s = new Student();
        s.setName(req.name());
        s.setDegree(req.degree());
        s.setEmail(req.email());
        s.setUsername(req.username());
        s.setPassword(passwordEncoder.encode(req.password()));
        s.setRole(Role.STUDENT);
        Student saved = studentRepository.save(s);
        String token = jwtService.issue(saved.getId(), saved.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, UserDto.from(saved)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var maybeStudent = studentRepository.findByUsername(req.username());
        if (maybeStudent.isEmpty()
                || !passwordEncoder.matches(req.password(), maybeStudent.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        Student s = maybeStudent.get();
        String token = jwtService.issue(s.getId(), s.getRole());
        return ResponseEntity.ok(new AuthResponse(token, UserDto.from(s)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWT — no-op handler. Hook for future token blocklisting.
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal Long authStudentId) {
        Student s = studentRepository.findById(authStudentId)
                .orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(UserDto.from(s));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMe(@AuthenticationPrincipal Long authStudentId,
                                            @Valid @RequestBody UpdateProfileRequest req) {
        Student s = studentRepository.findById(authStudentId)
                .orElseThrow(NotFoundException::new);
        if (req.email() != null && !req.email().equals(s.getEmail())) {
            if (studentRepository.existsByEmail(req.email())) {
                throw new ConflictException("Email already registered");
            }
            s.setEmail(req.email());
        }
        if (req.name() != null) s.setName(req.name());
        if (req.degree() != null) s.setDegree(req.degree());
        Student saved = studentRepository.save(s);
        return ResponseEntity.ok(UserDto.from(saved));
    }
}
