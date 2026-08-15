package com.example.backend_service.marketplace.service;

import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.BadRequestException;
import com.example.backend_service.common.exception.ConflictException;
import com.example.backend_service.common.verification.VerificationCodeGenerator;
import com.example.backend_service.marketplace.model.SellerEmailVerification;
import com.example.backend_service.marketplace.repository.SellerEmailVerificationRepository;
import com.example.backend_service.marketplace.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

/**
 * Verifies a seller's email during signup, before any Seller account exists. Mirrors
 * {@code jobhub.service.RecruiterEmailVerificationService} — see that class for the full
 * rationale.
 */
@Service
public class SellerEmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SellerEmailVerificationService.class);

    private static final long CODE_TTL_MINUTES = 10;
    private static final long VERIFICATION_TOKEN_TTL_MINUTES = 30;
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final SellerEmailVerificationRepository verificationRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeGenerator codeGenerator;

    public SellerEmailVerificationService(
            SellerEmailVerificationRepository verificationRepository,
            SellerRepository sellerRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("marketplaceEmailService") EmailService emailService,
            VerificationCodeGenerator codeGenerator) {
        this.verificationRepository = verificationRepository;
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.codeGenerator = codeGenerator;
    }

    public void sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Please enter a valid email address.");
        }
        if (sellerRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        SellerEmailVerification verification = verificationRepository.findById(email)
                .orElseGet(() -> new SellerEmailVerification(email));

        String code = codeGenerator.generateSixDigitCode();
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setCodeExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        verification.setCodeAttempts(0);
        verification.setVerificationTokenHash(null);
        verification.setVerificationTokenExpiresAt(null);
        verificationRepository.save(verification);

        emailService.sendHtml(email, "Verify your email for UniVerse Marketplace", buildCodeEmailHtml(code));
        log.info("Signup email verification code sent to email={}", email);
    }

    public String verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        SellerEmailVerification verification = verificationRepository.findById(email).orElse(null);
        if (verification == null || verification.getCodeHash() == null
                || verification.getCodeExpiresAt() == null
                || verification.getCodeExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        if (verification.getCodeAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new BadRequestException("Too many incorrect attempts. Please request a new code.");
        }

        if (code == null || code.isBlank() || !passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.setCodeAttempts(verification.getCodeAttempts() + 1);
            verificationRepository.save(verification);
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        String token = codeGenerator.generateOpaqueToken();
        verification.setVerificationTokenHash(passwordEncoder.encode(token));
        verification.setVerificationTokenExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        verification.setCodeHash(null);
        verification.setCodeExpiresAt(null);
        verification.setCodeAttempts(0);
        verificationRepository.save(verification);

        return token;
    }

    /**
     * Called from seller registration to redeem the token obtained via {@link #verifyCode}.
     */
    public void consumeVerification(String rawEmail, String token) {
        String email = normalize(rawEmail);
        SellerEmailVerification verification = verificationRepository.findById(email).orElse(null);
        if (verification == null || verification.getVerificationTokenHash() == null
                || verification.getVerificationTokenExpiresAt() == null
                || verification.getVerificationTokenExpiresAt().isBefore(Instant.now())
                || token == null || token.isBlank()
                || !passwordEncoder.matches(token, verification.getVerificationTokenHash())) {
            throw new BadRequestException("Please verify your email before submitting.");
        }
        verificationRepository.delete(verification);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String buildCodeEmailHtml(String code) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;">
                    <h2 style="color: #4f46e5;">UniVerse Marketplace</h2>
                    <p>Use the code below to verify this email address and continue creating your seller account. This code expires in %d minutes.</p>
                    <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px; background: #f3f4f6; padding: 16px 24px; border-radius: 12px; text-align: center; margin: 24px 0;">
                        %s
                    </div>
                    <p>If you didn't request this, you can safely ignore this email.</p>
                    <p style="color: #6b7280; font-size: 12px; margin-top: 32px;">UniVerse Marketplace &bull; Do not reply to this automated email.</p>
                </div>
                """.formatted(CODE_TTL_MINUTES, code);
    }
}
