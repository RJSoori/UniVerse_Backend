package com.example.backend_service.auth.service;

import com.example.backend_service.Student;
import com.example.backend_service.StudentRepository;
import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.BadRequestException;
import com.example.backend_service.common.verification.VerificationCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles the student "forgot password" flow. Mirrors
 * {@code jobhub.service.RecruiterPasswordResetService} exactly — see that class for the
 * full rationale (single-use bcrypt-hashed code, generic response to avoid email
 * enumeration, opaque reset token to consume the code exactly once).
 */
@Service
public class StudentPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(StudentPasswordResetService.class);

    private static final long CODE_TTL_MINUTES = 10;
    private static final long RESET_TOKEN_TTL_MINUTES = 10;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeGenerator codeGenerator;

    public StudentPasswordResetService(
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("studentEmailService") EmailService emailService,
            VerificationCodeGenerator codeGenerator) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.codeGenerator = codeGenerator;
    }

    public void requestReset(String rawEmail) {
        String email = normalize(rawEmail);
        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null) {
            log.info("Password reset requested for unknown student email={}", email);
            return;
        }

        String code = codeGenerator.generateSixDigitCode();
        student.setResetCodeHash(passwordEncoder.encode(code));
        student.setResetCodeExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        student.setResetCodeAttempts(0);
        student.setResetTokenHash(null);
        student.setResetTokenExpiresAt(null);
        studentRepository.save(student);

        emailService.sendHtml(student.getEmail(), "Your UniVerse verification code", buildCodeEmailHtml(student, code));
        log.info("Password reset code sent to student email={}", email);
    }

    public String verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null || student.getResetCodeHash() == null
                || student.getResetCodeExpiresAt() == null
                || student.getResetCodeExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        if (student.getResetCodeAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new BadRequestException("Too many incorrect attempts. Please request a new code.");
        }

        if (code == null || code.isBlank() || !passwordEncoder.matches(code, student.getResetCodeHash())) {
            student.setResetCodeAttempts(student.getResetCodeAttempts() + 1);
            studentRepository.save(student);
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        String resetToken = codeGenerator.generateOpaqueToken();
        student.setResetTokenHash(passwordEncoder.encode(resetToken));
        student.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        student.setResetCodeHash(null);
        student.setResetCodeExpiresAt(null);
        student.setResetCodeAttempts(0);
        studentRepository.save(student);

        return resetToken;
    }

    public void resetPassword(String rawEmail, String resetToken, String newPassword) {
        String email = normalize(rawEmail);
        Student student = studentRepository.findByEmail(email).orElse(null);
        if (student == null || student.getResetTokenHash() == null
                || student.getResetTokenExpiresAt() == null
                || student.getResetTokenExpiresAt().isBefore(Instant.now())
                || resetToken == null || resetToken.isBlank()
                || !passwordEncoder.matches(resetToken, student.getResetTokenHash())) {
            throw new BadRequestException("Invalid or expired session. Please restart the password reset process.");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        student.setPassword(passwordEncoder.encode(newPassword));
        student.setResetTokenHash(null);
        student.setResetTokenExpiresAt(null);
        studentRepository.save(student);
        log.info("Password reset completed for student email={}", email);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String buildCodeEmailHtml(Student student, String code) {
        String name = student.getName() != null && !student.getName().isBlank() ? student.getName() : "there";
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;">
                    <h2 style="color: #4f46e5;">UniVerse</h2>
                    <p>Hi %s,</p>
                    <p>We received a request to reset the password for your UniVerse account. Use the code below to continue. This code expires in %d minutes.</p>
                    <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px; background: #f3f4f6; padding: 16px 24px; border-radius: 12px; text-align: center; margin: 24px 0;">
                        %s
                    </div>
                    <p>If you didn't request this, you can safely ignore this email &mdash; your password will not be changed.</p>
                    <p style="color: #6b7280; font-size: 12px; margin-top: 32px;">UniVerse &bull; Do not reply to this automated email.</p>
                </div>
                """.formatted(name, CODE_TTL_MINUTES, code);
    }
}
