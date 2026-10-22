package com.example.backend_service.security;

import com.example.backend_service.jobhub.service.RecruiterJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class RecruiterJwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private RecruiterJwtService recruiterJwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String recruiterToken = request.getHeader("X-Recruiter-Token");

        if (recruiterToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long recruiterId = recruiterJwtService.parse(recruiterToken);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "recruiter:" + recruiterId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid recruiter token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
