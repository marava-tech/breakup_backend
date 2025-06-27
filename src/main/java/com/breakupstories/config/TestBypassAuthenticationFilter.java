package com.breakupstories.config;

import com.breakupstories.service.UserService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class TestBypassAuthenticationFilter extends OncePerRequestFilter {
    
    private final UserService userService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String bypassAuth = request.getHeader("X-BS-Authorization");
        String userId = request.getHeader("X-BS-UserId");
        
        // Check if test bypass headers are present
        if ("true".equalsIgnoreCase(bypassAuth) && userId != null && !userId.trim().isEmpty()) {
            try {
                log.info("Test bypass authentication activated for user ID: {}", userId);
                
                // Get user by ID
                var userResponse = userService.getUserById(userId);
                
                // Create UserDetails for the user
                UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                        .username(userResponse.getEmail())
                        .password("") // No password in this setup
                        .authorities("ROLE_" + userResponse.getRole().name())
                        .build();
                
                // Set authentication in security context
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Test bypass authentication successful for user: {}", userResponse.getEmail());
                
            } catch (Exception e) {
                log.warn("Test bypass authentication failed for user ID: {}. Error: {}", userId, e.getMessage());
                // Continue with normal authentication flow
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 