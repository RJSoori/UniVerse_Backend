package com.example.backend_service.auth.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.backend_service.Role;
import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
// @Component - REMOVED: Duplicate of com.example.backend_service.security.AdminBootstrapper
// This file should be deleted. The actual implementation is in security/ package.
/**
 * Seeds a single ADMIN-role student on startup if (a) zero admins exist and
 * (b) ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD env vars are set.
 * Idempotent: subsequent runs are no-ops once an admin exists.
 */
@Deprecated
public class AdminBootstrapper implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrapper(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap.username:}") String adminUsername,
            @Value("${app.admin.bootstrap.password:}") String adminPassword) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (studentRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("No admin exists; set ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD env vars to seed one.");
            return;
        }
        if (studentRepository.existsByUsername(adminUsername)) {
            log.warn("Admin bootstrap skipped: username '{}' already taken by a non-admin row.", adminUsername);
            return;
        }
        Student admin = new Student();
        admin.setName("Administrator");
        admin.setUsername(adminUsername);
        admin.setEmail(adminUsername + "@admin.local");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        studentRepository.save(admin);
        log.info("Bootstrapped ADMIN account '{}'.", adminUsername);
    }
}
