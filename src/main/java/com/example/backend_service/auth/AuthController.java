package com.example.backend_service.auth;

import com.example.backend_service.AzureBlobService;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${app.jwt.ttl-minutes:1440}")
    private long jwtTtlMinutes;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AzureBlobService azureBlobService;

    public AuthController(StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AzureBlobService azureBlobService) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.azureBlobService = azureBlobService;
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(jwtTtlMinutes * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                                  HttpServletResponse response) {
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
        setAuthCookie(response, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, UserDto.from(saved)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletResponse response) {
        var maybeStudent = studentRepository.findByUsername(req.username());
        if (maybeStudent.isEmpty()
                || !passwordEncoder.matches(req.password(), maybeStudent.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        Student s = maybeStudent.get();
        String token = jwtService.issue(s.getId(), s.getRole());
        setAuthCookie(response, token);
        return ResponseEntity.ok(new AuthResponse(token, UserDto.from(s)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearAuthCookie(response);
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

    @PostMapping("/me/photo")
    public ResponseEntity<UserDto> updateProfilePicture(
            @AuthenticationPrincipal Long authStudentId,
            @RequestParam("photo") MultipartFile photo) throws IOException {
        Student s = studentRepository.findById(authStudentId)
                .orElseThrow(NotFoundException::new);
        String imageUrl = azureBlobService.uploadFile(photo);
        s.setProfilePictureUrl(imageUrl);
        Student saved = studentRepository.save(s);
        return ResponseEntity.ok(UserDto.from(saved));
    }
}
