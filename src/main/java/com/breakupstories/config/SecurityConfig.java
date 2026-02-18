package com.breakupstories.config;

import com.breakupstories.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final TestBypassAuthenticationFilter testBypassAuthFilter;
    private final AdminAuthorizationFilter adminAuthorizationFilter;
    private final UserService userService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Infrastructure
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .requestMatchers("/api/configs/device-configs").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // Stories — public reads (my-stories must come before the wildcard)
                        .requestMatchers(HttpMethod.GET, "/api/stories/my-stories").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/stories",
                                "/api/stories/type",
                                "/api/stories/search",
                                "/api/stories/*").permitAll()

                        // Stories — auth required for writes
                        .requestMatchers(HttpMethod.POST, "/api/stories", "/api/stories/written").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/stories/*/like").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/stories/*/like").authenticated()

                        // Comments — public reads, auth required for writes
                        .requestMatchers(HttpMethod.GET, "/api/comments/story/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()

                        // Admin
                        .requestMatchers("/api/audits/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // User-specific
                        .requestMatchers("/api/configs/**").authenticated()
                        .requestMatchers("/api/users/profile").authenticated()
                        .requestMatchers("/api/users/profile-image").authenticated()
                        .requestMatchers("/api/users/preferred-language").authenticated()
                        .requestMatchers("/api/users/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(testBypassAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }
} 