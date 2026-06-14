package com.example.backend_service.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads Authorization: Bearer <jwt>, validates, and populates SecurityContext
 * with principal = Long studentId, authority = ROLE_<role>.
 *
 * Short-circuits OPTIONS so CORS preflight passes through unauthenticated.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Extract JWT: prefer httpOnly cookie, fall back to Authorization: Bearer header
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("auth_token".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }
        if (token != null) {
            try {
                JwtService.ParsedToken parsed = jwtService.parse(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        parsed.studentId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role().name())));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
