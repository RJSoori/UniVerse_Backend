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
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          SellerJwtAuthFilter sellerJwtAuthFilter,
                          RecruiterJwtAuthFilter recruiterJwtAuthFilter,
                          RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.sellerJwtAuthFilter = sellerJwtAuthFilter;
        this.recruiterJwtAuthFilter = recruiterJwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
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
                        // Student forgot-password / signup email verification
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-reset-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/email/send-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/email/verify-code").permitAll()
                        // Seller auth (marketplace)
                        .requestMatchers("/api/marketplace/sellers/register", "/api/marketplace/sellers/login").permitAll()
                        // Seller forgot-password / signup email verification
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/sellers/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/sellers/verify-reset-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/sellers/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/sellers/email/send-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/marketplace/sellers/email/verify-code").permitAll()
                        // Public job browsing
                        .requestMatchers(HttpMethod.GET, "/api/jobs/all").permitAll()
                        // Recruiter registration and login
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters").permitAll()
                        .requestMatchers("/api/jobs/recruiters/login").permitAll()
                        // Recruiter forgot-password flow
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters/verify-reset-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters/reset-password").permitAll()
                        // Recruiter signup email verification
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters/email/send-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/recruiters/email/verify-code").permitAll()
                        // GPA health check
                        .requestMatchers("/api/gpa/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Recruiter self-service profile (Portal Settings)
                        .requestMatchers(HttpMethod.GET, "/api/jobs/recruiters/me").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.PUT, "/api/jobs/recruiters/me").hasRole("RECRUITER")
                        // Recruiter-only job actions
                        .requestMatchers(HttpMethod.POST, "/api/jobs/post").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.GET, "/api/jobs/recruiters/*/jobs").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.PATCH, "/api/jobs/recruiters/*/jobs/*/active").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.PUT, "/api/jobs/recruiters/*/jobs/*").hasRole("RECRUITER")
                        .requestMatchers(HttpMethod.DELETE, "/api/jobs/recruiters/*/jobs/*").hasRole("RECRUITER")
                        // Everything else requires a student JWT
                        .anyRequest().authenticated())
                // Order matters: recruiter/seller tokens are explicit, single-purpose headers a
                // caller deliberately attaches, whereas the student auth_token cookie rides along
                // on every request regardless of intent (apiFetch always sends credentials:
                // "include"). Recruiter/seller must get first claim on the identity slot - each
                // guards on "nothing has authenticated yet" - so a stray valid student cookie on a
                // recruiter/seller-portal request can never pre-empt that request's own token.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(sellerJwtAuthFilter, JwtAuthFilter.class)
                .addFilterBefore(recruiterJwtAuthFilter, SellerJwtAuthFilter.class)
                .addFilterBefore(rateLimitFilter, RecruiterJwtAuthFilter.class);
        return http.build();
    }
}
