package com.example.backend_service.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend_service.Role;
import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.auth.dto.AuthResponse;
import com.example.backend_service.auth.dto.LoginRequest;
import com.example.backend_service.auth.dto.RegisterRequest;
import com.example.backend_service.auth.dto.UpdateProfileRequest;
import com.example.backend_service.auth.dto.UserDto;
import com.example.backend_service.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController controller;

    @Test
    void register_createsStudentAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Nina", "CS", "nina@example.com", "nina", "secret12");
        Student saved = new Student();
        saved.setId(7L);
        saved.setName("Nina");
        saved.setDegree("CS");
        saved.setEmail("nina@example.com");
        saved.setUsername("nina");
        saved.setPassword("encoded");
        saved.setRole(Role.STUDENT);
        saved.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0));

        when(studentRepository.existsByUsername("nina")).thenReturn(false);
        when(studentRepository.existsByEmail("nina@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret12")).thenReturn("encoded");
        when(studentRepository.save(any(Student.class))).thenReturn(saved);
        when(jwtService.issue(7L, Role.STUDENT)).thenReturn("token-7");

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("token-7");
        assertThat(response.getBody().user()).isEqualTo(new UserDto(7L, "nina", "Nina", "nina@example.com", "CS", Role.STUDENT, LocalDateTime.of(2025, 1, 1, 0, 0, 0)));
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void login_rejectsInvalidPassword() {
        when(studentRepository.findByUsername("nina")).thenReturn(Optional.of(new Student()));
        when(passwordEncoder.matches("wrong", null)).thenReturn(false);

        ResponseEntity<?> response = controller.login(new LoginRequest("nina", "wrong"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtService, never()).issue(any(), any());
    }

    @Test
    void me_returnsCurrentStudent() {
        Student saved = new Student();
        saved.setId(7L);
        saved.setUsername("nina");
        saved.setName("Nina");
        saved.setEmail("nina@example.com");
        saved.setDegree("CS");
        saved.setRole(Role.STUDENT);

        when(studentRepository.findById(7L)).thenReturn(Optional.of(saved));

        ResponseEntity<UserDto> response = controller.me(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(UserDto.from(saved));
    }

    @Test
    void updateMe_updatesAllowedFields() {
        Student saved = new Student();
        saved.setId(7L);
        saved.setUsername("nina");
        saved.setName("Nina");
        saved.setEmail("nina@example.com");
        saved.setDegree("CS");
        saved.setRole(Role.STUDENT);
        saved.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0));

        when(studentRepository.findById(7L)).thenReturn(Optional.of(saved));
        when(studentRepository.existsByEmail("nina.new@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<UserDto> response = controller.updateMe(7L, new UpdateProfileRequest("Nina New", "nina.new@example.com", "IT"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new UserDto(7L, "nina", "Nina New", "nina.new@example.com", "IT", Role.STUDENT, LocalDateTime.of(2025, 1, 1, 0, 0, 0)));
    }
}