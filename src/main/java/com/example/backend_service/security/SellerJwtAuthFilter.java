package com.example.backend_service.security;

import com.example.backend_service.marketplace.service.SellerJwtService;
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
public class SellerJwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private SellerJwtService sellerJwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String sellerToken = request.getHeader("X-Seller-Token");

        if (sellerToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long sellerId = sellerJwtService.parse(sellerToken);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "seller:" + sellerId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SELLER"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid seller token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}