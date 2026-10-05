package com.securevault.repository;

import com.securevault.entity.UserSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository
        extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionId(String sessionId);

    List<UserSession> findByEmailOrderByCreatedAtDesc(
            String email
    );
}