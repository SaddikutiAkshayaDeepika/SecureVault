package com.securevault.security;

import com.securevault.entity.User;
import com.securevault.service.SessionService;
import com.securevault.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserService userService;
    private final SessionService sessionService;

    public OAuth2SuccessHandler(
            JwtService jwtService,
            UserService userService,
            SessionService sessionService) {

        this.jwtService = jwtService;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oauthUser.getAttribute("email");

        if (email == null ||
                email.trim().isEmpty()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Google account email not available"
            );

            return;
        }

        User user;

        try {

            user =
                    userService.getUserByEmail(email);

        } catch (Exception e) {

            User newUser =
                    new User();

            newUser.setEmail(email);

            newUser.setPassword(
                    UUID.randomUUID().toString()
            );

            user =
                    userService.registerUser(newUser);
        }

        String ipAddress =
                request.getRemoteAddr();

        String device =
                request.getHeader("User-Agent");

        String sessionId =
                sessionService.createSession(
                        user.getEmail(),
                        device,
                        ipAddress
                );

        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole().name(),
                        sessionId
                );

        String frontendUrl =
                System.getenv("FRONTEND_URL");

        if (frontendUrl == null ||
                frontendUrl.isBlank()) {

            frontendUrl =
                    "http://localhost:5177";
        }

        String redirectUrl =
                frontendUrl
                        + "/oauth-success?token="
                        + URLEncoder.encode(
                                token,
                                StandardCharsets.UTF_8
                        );

        response.sendRedirect(redirectUrl);
    }
}