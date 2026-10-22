package com.example.backend_service.security;

import com.example.backend_service.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Service for issuing and parsing JWTs.
 * Issues and verifies HS256 JWTs. Subject = student id (Long, serialized as string).
 * Custom claim "role" carries the student's role for authorization.
 */
@Service
public class JwtService {

    private final String secret;
    private final long ttlMinutes;
    private SecretKey signingKey;

    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.ttl-minutes:1440}") long ttlMinutes) {
        this.secret = secret;
        this.ttlMinutes = ttlMinutes;
    }

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET env var is required. Generate with `openssl rand -base64 64`.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes for HS256. Generate with `openssl rand -base64 64`.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issue(Long studentId, Role role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);
        return Jwts.builder()
                .subject(String.valueOf(studentId))
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public ParsedToken parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long studentId = Long.parseLong(claims.getSubject());
        Role role = Role.valueOf(claims.get("role", String.class));
        return new ParsedToken(studentId, role);
    }

    public record ParsedToken(Long studentId, Role role) {}
}
