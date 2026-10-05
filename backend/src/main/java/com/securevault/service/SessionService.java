package com.securevault.service;

import com.securevault.entity.UserSession;
import com.securevault.repository.UserSessionRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final UserSessionRepository sessionRepository;

    public SessionService(
            UserSessionRepository sessionRepository) {

        this.sessionRepository = sessionRepository;
    }

    public String createSession(
            String email,
            String device,
            String ipAddress) {

        String sessionId =
                UUID.randomUUID().toString();

        UserSession session =
                new UserSession();

        session.setSessionId(sessionId);
        session.setEmail(email);
        session.setDevice(device);
        session.setIpAddress(ipAddress);
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(
                Instant.now()
                        .plus(24, ChronoUnit.HOURS)
        );
        session.setRevoked(false);

        sessionRepository.save(session);

        return sessionId;
    }

    public boolean isSessionActive(
            String sessionId) {

        if (sessionId == null) {
            return false;
        }

        UserSession session =
                sessionRepository
                        .findBySessionId(sessionId)
                        .orElse(null);

        if (session == null) {
            return false;
        }

        if (session.isRevoked()) {
            return false;
        }

        if (session.getExpiresAt()
                .isBefore(Instant.now())) {

            return false;
        }

        return true;
    }

    public List<UserSession> getSessions(
            String email) {

        return sessionRepository
                .findByEmailOrderByCreatedAtDesc(email);
    }

    public void revokeSession(
            String sessionId,
            String email) {

        UserSession session =
                sessionRepository
                        .findBySessionId(sessionId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Session not found"
                                )
                        );

        if (!session.getEmail()
                .equalsIgnoreCase(email)) {

            throw new IllegalArgumentException(
                    "You can only revoke your own sessions"
            );
        }

        session.setRevoked(true);

        sessionRepository.save(session);
    }

    public void revokeAllSessions(
            String email) {

        List<UserSession> sessions =
                sessionRepository
                        .findByEmailOrderByCreatedAtDesc(email);

        for (UserSession session : sessions) {
            session.setRevoked(true);
        }

        sessionRepository.saveAll(sessions);
    }
}