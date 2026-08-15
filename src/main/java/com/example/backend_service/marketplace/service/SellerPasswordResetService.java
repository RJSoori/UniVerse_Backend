package com.example.backend_service.marketplace.service;

import com.example.backend_service.common.email.EmailService;
import com.example.backend_service.common.exception.BadRequestException;
import com.example.backend_service.common.verification.VerificationCodeGenerator;
import com.example.backend_service.marketplace.model.Seller;
import com.example.backend_service.marketplace.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handles the seller "forgot password" flow. Mirrors
 * {@code jobhub.service.RecruiterPasswordResetService} — see that class for the full
 * rationale.
 */
@Service
public class SellerPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(SellerPasswordResetService.class);

    private static final long CODE_TTL_MINUTES = 10;
    private static final long RESET_TOKEN_TTL_MINUTES = 10;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeGenerator codeGenerator;

    public SellerPasswordResetService(
            SellerRepository sellerRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("marketplaceEmailService") EmailService emailService,
            VerificationCodeGenerator codeGenerator) {
        this.sellerRepository = sellerRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.codeGenerator = codeGenerator;
    }

    public void requestReset(String rawEmail) {
        String email = normalize(rawEmail);
        Seller seller = sellerRepository.findByEmail(email).orElse(null);
        if (seller == null) {
            log.info("Password reset requested for unknown seller email={}", email);
            return;
        }

        String code = codeGenerator.generateSixDigitCode();
        seller.setResetCodeHash(passwordEncoder.encode(code));
        seller.setResetCodeExpiresAt(Instant.now().plus(CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        seller.setResetCodeAttempts(0);
        seller.setResetTokenHash(null);
        seller.setResetTokenExpiresAt(null);
        sellerRepository.save(seller);

        emailService.sendHtml(seller.getEmail(), "Your UniVerse Marketplace verification code",
                buildCodeEmailHtml(seller, code));
        log.info("Password reset code sent to seller email={}", email);
    }

    public String verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        Seller seller = sellerRepository.findByEmail(email).orElse(null);
        if (seller == null || seller.getResetCodeHash() == null
                || seller.getResetCodeExpiresAt() == null
                || seller.getResetCodeExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        if (seller.getResetCodeAttempts() >= MAX_CODE_ATTEMPTS) {
            throw new BadRequestException("Too many incorrect attempts. Please request a new code.");
        }

        if (code == null || code.isBlank() || !passwordEncoder.matches(code, seller.getResetCodeHash())) {
            seller.setResetCodeAttempts(seller.getResetCodeAttempts() + 1);
            sellerRepository.save(seller);
            throw new BadRequestException("Invalid or expired code. Please request a new one.");
        }

        String resetToken = codeGenerator.generateOpaqueToken();
        seller.setResetTokenHash(passwordEncoder.encode(resetToken));
        seller.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
        seller.setResetCodeHash(null);
        seller.setResetCodeExpiresAt(null);
        seller.setResetCodeAttempts(0);
        sellerRepository.save(seller);

        return resetToken;
    }

    public void resetPassword(String rawEmail, String resetToken, String newPassword) {
        String email = normalize(rawEmail);
        Seller seller = sellerRepository.findByEmail(email).orElse(null);
        if (seller == null || seller.getResetTokenHash() == null
                || seller.getResetTokenExpiresAt() == null
                || seller.getResetTokenExpiresAt().isBefore(Instant.now())
                || resetToken == null || resetToken.isBlank()
                || !passwordEncoder.matches(resetToken, seller.getResetTokenHash())) {
            throw new BadRequestException("Invalid or expired session. Please restart the password reset process.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters.");
        }

        seller.setPassword(passwordEncoder.encode(newPassword));
        seller.setResetTokenHash(null);
        seller.setResetTokenExpiresAt(null);
        sellerRepository.save(seller);
        log.info("Password reset completed for seller email={}", email);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String buildCodeEmailHtml(Seller seller, String code) {
        String name = seller.getStoreName() != null && !seller.getStoreName().isBlank() ? seller.getStoreName() : "there";
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1f2937;">
                    <h2 style="color: #4f46e5;">UniVerse Marketplace</h2>
                    <p>Hi %s,</p>
                    <p>We received a request to reset the password for your seller account. Use the code below to continue. This code expires in %d minutes.</p>
                    <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px; background: #f3f4f6; padding: 16px 24px; border-radius: 12px; text-align: center; margin: 24px 0;">
                        %s
                    </div>
                    <p>If you didn't request this, you can safely ignore this email &mdash; your password will not be changed.</p>
                    <p style="color: #6b7280; font-size: 12px; margin-top: 32px;">UniVerse Marketplace &bull; Do not reply to this automated email.</p>
                </div>
                """.formatted(name, CODE_TTL_MINUTES, code);
    }
}
