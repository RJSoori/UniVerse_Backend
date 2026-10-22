package com.example.backend_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles brute-force attempts against authentication endpoints. Tracks a sliding
 * window of request timestamps per client IP + path, independent of the three JWT
 * auth filters (which only run for requests that carry a token).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Keyed by IP+path (not account) - deliberately so, since scoping by account would let an
    // attacker spread guesses across many target emails from one IP to dodge the limit. Raised
    // from 5 to 20: still far too slow to make password brute-forcing practical, but no longer
    // trips over normal multi-account developer testing from a single machine.
    private static final int MAX_ATTEMPTS = 20;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/verify-reset-code",
            "/api/auth/reset-password",
            "/api/auth/email/send-code",
            "/api/auth/email/verify-code",
            "/api/marketplace/sellers/login",
            "/api/marketplace/sellers/register",
            "/api/marketplace/sellers/forgot-password",
            "/api/marketplace/sellers/verify-reset-code",
            "/api/marketplace/sellers/reset-password",
            "/api/marketplace/sellers/email/send-code",
            "/api/marketplace/sellers/email/verify-code",
            "/api/jobs/recruiters/login",
            "/api/jobs/recruiters",
            "/api/jobs/recruiters/forgot-password",
            "/api/jobs/recruiters/verify-reset-code",
            "/api/jobs/recruiters/reset-password",
            "/api/jobs/recruiters/email/send-code",
            "/api/jobs/recruiters/email/verify-code"
    );

    private final ConcurrentHashMap<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientIp(request) + ":" + request.getRequestURI();
        if (isRateLimited(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many attempts. Please try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = attemptsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_ATTEMPTS) {
                return true;
            }
            timestamps.addLast(now);
        }
        return false;
    }

    private String clientIp(HttpServletRequest request) {
        // Deliberately ignores X-Forwarded-For: it is client-controlled unless a trusted
        // reverse proxy overwrites it, which this deployment does not configure, and trusting
        // it here would let an attacker reset their own rate limit by spoofing the header.
        return request.getRemoteAddr();
    }
}
