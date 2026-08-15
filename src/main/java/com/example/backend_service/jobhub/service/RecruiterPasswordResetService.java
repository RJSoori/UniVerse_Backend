package com.example.backend_service.jobhub.service;

import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.BadRequestException;
import com.example.backend_service.common.verification.VerificationCodeGenerator;
import com.example.backend_service.jobhub.model.Recruiter;
import com.example.backend_service.jobhub.repository.RecruiterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles the recruiter "forgot password" flow:
 *   1. requestReset  - looks up the recruiter by email and, if found, emails a 6-digit code.
 *   2. verifyCode     - checks the code and hands back a short-lived opaque reset token.
 *   3. resetPassword  - consumes the reset token and sets the new password.
 *
 * Both the code and the reset token are stored only as bcrypt hashes (never in plaintext),
 * mirroring how account passwords are stored.
 */
@Service
public class RecruiterPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(RecruiterPasswordResetService.class);

    private static final long CODE_TTL_MINUTES = 10;
    private static final long RESET_TOKEN_TTL_MINUTES = 10;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeGenerator codeGenerator;

    public RecruiterPasswordResetService(
            RecruiterRepository recruiterRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("jobhubEmailService") EmailService emailService,
            VerificationCodeGenerator codeGenerator) {
        this.recruiterRepository = recruiterRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.codeGenerator = codeGenerator;
    }

    /**
     * Generates and emails a 6-digit reset code if the email matches a recruiter account.
     * Deliberately silent (no exception, no "not found" signal) when there's no match, so the
     * caller can always show the same generic "check your email" message and avoid leaking
     * which addresses are registered.
     */
    public void requestReset(String rawEmail) {
        String email = normalize(rawEmail);
        Recruiter recruiter = recruiterRepository.findByEmail(email);
        if (recruiter == null) {
            log.info("Password reset requested for unknown recruiter email={}", email);
            return;
        }

        String code = codeGenerator.generateSixDigitCode();
        recruiter.setResetCodeHash(passwordEncoder.encode(code));
        recruiter.setResetCodeExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        recruiter.setResetCodeAttempts(0);
        // Invalidate any previously-issued reset token so an old, already-verified session
        // can't be used once a new code has been requested.
        recruiter.setResetTokenHash(null);
        recruiter.setResetTokenExpiresAt(null);
        recruiterRepository.save(recruiter);

        emailService.sendHtml(recruiter.getEmail(), "Your UniVerse JobHub verification code",
                buildCodeEmailHtml(recruiter, code));
        log.info("Password reset code sent to recruiter email={}", email);
    }

    /**
     * Validates the 6-digit code and, on success, issues a short-lived opaque reset token that
     * the frontend must present to the final reset-password step. The code itself is single-use:
     * it's cleared as soon as it's verified, whether or not the token ends up being redeemed.
     */
    public String verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        Recruiter recruiter = recruiterRepository.findByEmail(email);
        if (recruiter == null || recruiter.getResetCodeHash() == null
                || recruiter.getResetCodeExpiresAt() == null
                || recruiter.getResetCodeExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        if (recruiter.getResetCodeAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new BadRequestException("Too many incorrect attempts. Please request a new code.");
        }

        if (code == null || code.isBlank() || !passwordEncoder.matches(code, recruiter.getResetCodeHash())) {
            recruiter.setResetCodeAttempts(recruiter.getResetCodeAttempts() + 1);
            recruiterRepository.save(recruiter);
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        String resetToken = codeGenerator.generateOpaqueToken();
        recruiter.setResetTokenHash(passwordEncoder.encode(resetToken));
        recruiter.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        // Consume the code so it can't be reused.
        recruiter.setResetCodeHash(null);
        recruiter.setResetCodeExpiresAt(null);
        recruiter.setResetCodeAttempts(0);
        recruiterRepository.save(recruiter);

        return resetToken;
    }

    /**
     * Redeems the reset token issued by {@link #verifyCode} and sets the new password.
     */
    public void resetPassword(String rawEmail, String resetToken, String newPassword) {
        String email = normalize(rawEmail);
        Recruiter recruiter = recruiterRepository.findByEmail(email);
        if (recruiter == null || recruiter.getResetTokenHash() == null
                || recruiter.getResetTokenExpiresAt() == null
                || recruiter.getResetTokenExpiresAt().isBefore(Instant.now())
                || resetToken == null || resetToken.isBlank()
                || !passwordEncoder.matches(resetToken, recruiter.getResetTokenHash())) {
            throw new BadRequestException("Invalid or expired session. Please restart the password reset process.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters.");
        }

        recruiter.setPassword(passwordEncoder.encode(newPassword));
        recruiter.setResetTokenHash(null);
        recruiter.setResetTokenExpiresAt(null);
        recruiterRepository.save(recruiter);
        log.info("Password reset completed for recruiter email={}", email);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String buildCodeEmailHtml(Recruiter recruiter, String code) {
        String name = recruiter.getContactPerson() != null && !recruiter.getContactPerson().isBlank()
                ? recruiter.getContactPerson()
                : recruiter.getCompanyName();
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;">
                    <h2 style="color: #4f46e5;">UniVerse JobHub</h2>
                    <p>Hi %s,</p>
                    <p>We received a request to reset the password for your recruiter account. Use the code below to continue. This code expires in %d minutes.</p>
                    <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px; background: #f3f4f6; padding: 16px 24px; border-radius: 12px; text-align: center; margin: 24px 0;">
                        %s
                    </div>
                    <p>If you didn't request this, you can safely ignore this email &mdash; your password will not be changed.</p>
                    <p style="color: #6b7280; font-size: 12px; margin-top: 32px;">UniVerse JobHub &bull; Do not reply to this automated email.</p>
                </div>
                """.formatted(name, CODE_TTL_MINUTES, code);
    }
}
