package com.example.backend_service.marketplace.service;

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

@Service
public class SellerJwtService {

    private final String secret;
    private final long ttlMinutes;
    private SecretKey signingKey;

    public SellerJwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.ttl-minutes:1440}") long ttlMinutes) {
        this.secret = secret + "_seller";
        this.ttlMinutes = ttlMinutes;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret too short for seller tokens.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issue(Long sellerId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);
        return Jwts.builder()
                .subject(String.valueOf(sellerId))
                .claim("type", "SELLER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public Long parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}