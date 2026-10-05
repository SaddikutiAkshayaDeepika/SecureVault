
package com.securevault.security;

import com.securevault.service.SessionService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            SessionService sessionService) {

        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        // No Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7);

        try {

            // Validate JWT and make sure MFA is completed
            if (jwtService.isTokenValid(token) &&
                    jwtService.isMfaVerified(token)) {

                // Get session ID from JWT
                String sessionId =
                        jwtService.extractSessionId(token);

                // Session must exist and must not be revoked/expired
                if (sessionId == null ||
                        !sessionService.isSessionActive(sessionId)) {

                    SecurityContextHolder.clearContext();

                    filterChain.doFilter(
                            request,
                            response
                    );

                    return;
                }

                // Get user email
                String email =
                        jwtService.extractEmail(token);

                // Get user role
                String role =
                        jwtService.extractRole(token);

                if (role == null ||
                        role.trim().isEmpty()) {

                    role = "USER";
                }

                // Create Spring Security authority
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(
                                "ROLE_" + role
                        );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(
                                        authority
                                )
                        );

                // Store authentication in SecurityContext
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);

                // Make values available to controllers
                request.setAttribute(
                        "email",
                        email
                );

                request.setAttribute(
                        "sessionId",
                        sessionId
                );
            }

        } catch (Exception e) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}

