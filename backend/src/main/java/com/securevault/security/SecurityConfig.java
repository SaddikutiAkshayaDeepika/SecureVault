package com.securevault.security;

import com.securevault.service.SessionService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.web.util.matcher.RequestMatcher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final OAuth2SuccessHandler oauth2SuccessHandler;

    public SecurityConfig(
            JwtService jwtService,
            SessionService sessionService,
            OAuth2SuccessHandler oauth2SuccessHandler) {

        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtService,
                        sessionService
                );

        // Match only REST API requests
        RequestMatcher apiMatcher =
                request -> request
                        .getRequestURI()
                        .startsWith("/api/");

        http
                // =========================
                // CSRF
                // =========================
                .csrf(csrf -> csrf.disable())

                // =========================
                // CORS
                // =========================
                .cors(cors -> {})

                // =========================
                // API AUTHENTICATION ERROR
                // =========================
                .exceptionHandling(exception ->
                        exception.defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {

                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );

                                    response.setContentType(
                                            "text/plain;charset=UTF-8"
                                    );

                                    response.getWriter().write(
                                            "Unauthorized"
                                    );
                                },
                                apiMatcher
                        )
                )

                // =========================
                // AUTHORIZATION
                // =========================
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/",
                                "/api/test",
                                "/error",
                                "/oauth2/**",
                                "/login/**"
                        )
                        .permitAll()

                        // Password generator
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/password/generate"
                        )
                        .permitAll()

                        // Authentication endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/mfa-login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        )
                        .permitAll()

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        // Everything else
                        .anyRequest()
                        .authenticated()
                )

                // =========================
                // GOOGLE OAUTH2
                // =========================
                .oauth2Login(oauth2 ->
                        oauth2.successHandler(
                                oauth2SuccessHandler
                        )
                )

                // =========================
                // JWT FILTER
                // =========================
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================
    // CORS CONFIGURATION
    // =========================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        String frontendUrl = System.getenv("FRONTEND_URL");

        List<String> allowedOrigins = new java.util.ArrayList<>(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "http://localhost:5175",
                        "http://localhost:5176",
                        "http://localhost:5177"
                )
        );

        if (frontendUrl != null && !frontendUrl.isBlank()) {
            allowedOrigins.add(frontendUrl);
        }

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of("*")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}