package com.securevault.service;

import com.securevault.entity.PasswordHistory;
import com.securevault.entity.User;
import com.securevault.repository.PasswordHistoryRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordHistoryService {

    private final PasswordHistoryRepository passwordHistoryRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public PasswordHistoryService(
            PasswordHistoryRepository passwordHistoryRepository) {
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    public void savePasswordHistory(
            User user,
            String passwordHash) {

        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPasswordHash(passwordHash);

        passwordHistoryRepository.save(history);
    }

    public boolean wasPasswordUsedBefore(
            User user,
            String newPassword) {

        List<PasswordHistory> historyList =
                passwordHistoryRepository
                        .findByUserOrderByCreatedAtDesc(user);

        for (PasswordHistory history : historyList) {
            if (passwordEncoder.matches(
                    newPassword,
                    history.getPasswordHash())) {

                return true;
            }
        }

        return false;
    }
}
