package com.example.backend_service.common.verification;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates the two primitives used across email-verification-style flows (recruiter
 * password reset, recruiter signup email verification, and any future ones):
 *   - a human-typeable 6-digit code, sent to the user via email
 *   - an opaque URL-safe token, handed to the frontend after a code is verified so it
 *     doesn't need to keep resubmitting the raw code for subsequent steps
 */
@Component
public class VerificationCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSixDigitCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    public String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
