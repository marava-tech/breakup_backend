package com.breakupstories.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorizationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String adminHeader = request.getHeader("X-BS-Admin");
        
        // Get current authentication
        var currentAuth = SecurityContextHolder.getContext().getAuthentication();
        
        if (currentAuth != null && currentAuth.isAuthenticated()) {
            // Check if user already has ADMIN role
            boolean hasAdminRole = currentAuth.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
            
            // If user already has ADMIN role, allow them to proceed
            if (hasAdminRole) {
                log.debug("User {} already has ADMIN role, proceeding", currentAuth.getName());
                filterChain.doFilter(request, response);
                return;
            }
            
            // Check if admin header is present and set to true (for testing/override)
            if ("true".equalsIgnoreCase(adminHeader)) {
                // Create new authentication with admin authority
                UserDetails userDetails = (UserDetails) currentAuth.getPrincipal();
                
                UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(() -> "ROLE_ADMIN")
                );
                
                SecurityContextHolder.getContext().setAuthentication(adminAuth);
                log.info("Admin authorization granted for user: {}", userDetails.getUsername());
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 