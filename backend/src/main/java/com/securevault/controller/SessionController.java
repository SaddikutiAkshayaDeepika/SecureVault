package com.securevault.controller;

import com.securevault.entity.UserSession;
import com.securevault.service.SessionService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(
            SessionService sessionService) {

        this.sessionService = sessionService;
    }

    // =========================
    // GET ALL MY SESSIONS
    // =========================

    @GetMapping
    public ResponseEntity<?> getMySessions() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        List<UserSession> sessions =
                sessionService.getSessions(email);

        return ResponseEntity.ok(sessions);
    }

    // =========================
    // REVOKE ONE SESSION
    // =========================

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> revokeSession(
            @PathVariable String sessionId) {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        try {

            sessionService.revokeSession(
                    sessionId,
                    email
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Session revoked successfully"
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    // =========================
    // LOGOUT ALL SESSIONS
    // =========================

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        sessionService.revokeAllSessions(email);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "All sessions revoked successfully"
                )
        );
    }
}