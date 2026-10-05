package com.securevault.service;

import com.securevault.entity.PasswordHistory;
import com.securevault.entity.User;
import com.securevault.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordHistoryServiceTest {

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    @InjectMocks
    private PasswordHistoryService passwordHistoryService;

    // 1. Save password history
    @Test
    void savePasswordHistory_shouldSavePassword() {
        User user = new User();

        passwordHistoryService.savePasswordHistory(
                user,
                "$2a$10$hashedPassword"
        );

        verify(passwordHistoryRepository).save(any(PasswordHistory.class));
    }

    // 2. Detect previously used password
    @Test
    void wasPasswordUsedBefore_shouldReturnTrueForMatchingPassword() {
        User user = new User();

        PasswordHistory history = new PasswordHistory();
        history.setUser(user);

        String password = "Password123";

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        history.setPasswordHash(encoder.encode(password));

        when(passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(history));

        assertTrue(
                passwordHistoryService.wasPasswordUsedBefore(user, password)
        );
    }

    // 3. Password was not used before
    @Test
    void wasPasswordUsedBefore_shouldReturnFalseForNewPassword() {
        User user = new User();

        PasswordHistory history = new PasswordHistory();
        history.setUser(user);

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        history.setPasswordHash(encoder.encode("OldPassword123"));

        when(passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(history));

        assertFalse(
                passwordHistoryService.wasPasswordUsedBefore(
                        user,
                        "NewPassword456"
                )
        );
    }
}
