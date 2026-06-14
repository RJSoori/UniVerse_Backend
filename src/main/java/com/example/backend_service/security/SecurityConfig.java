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
    private final RecruiterJwtAuthFilter recruiterJwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          SellerJwtAuthFilter sellerJwtAuthFilter,
                          RecruiterJwtAuthFilter recruiterJwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.sellerJwtAuthFilter = sellerJwtAuthFilter;
        this.recruiterJwtAuthFilter = recruiterJwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Student auth
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        // Seller auth (marketplace)
                        .requestMatchers("/api/marketplace/sellers/register", "/api/marketplace/sellers/login").permitAll()
                        // Public job browsing
                        .requestMatchers(HttpMethod.GET, "/api/jobs/all").permitAll()
                        // Recruiter registration and login
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters").permitAll()
                        .requestMatchers("/api/jobs/recruiters/login").permitAll()
                        // GPA health check
                        .requestMatchers("/api/gpa/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Recruiter-only job actions
                        .requestMatchers(HttpMethod.POST, "/api/jobs/post").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.DELETE, "/api/jobs/recruiters/*/jobs/*").hasRole("RECRUITER")
                        // Everything else requires a student JWT
                        .anyRequest().authenticated())
                .addFilterBefore(recruiterJwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sellerJwtAuthFilter, RecruiterJwtAuthFilter.class)
                .addFilterBefore(jwtAuthFilter, SellerJwtAuthFilter.class);
        return http.build();
    }
}
