package com.example.backend_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SellerJwtAuthFilter sellerJwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, SellerJwtAuthFilter sellerJwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.sellerJwtAuthFilter = sellerJwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure security: CORS enabled, CSRF disabled, stateless sessions, JWT auth, and endpoint permissions
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/marketplace/sellers/register", "/api/marketplace/sellers/login").permitAll()
                        .requestMatchers("/api/jobs/post", "/api/jobs/all", "/api/jobs/recruiters", "/api/jobs/recruiters/**").permitAll()
                        .requestMatchers("/api/gpa/health").permitAll()
                        .requestMatchers("/api/focus/save", "/api/focus/analytics").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sellerJwtAuthFilter, JwtAuthFilter.class);
        return http.build();
    }
}