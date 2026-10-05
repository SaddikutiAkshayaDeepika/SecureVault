package com.securevault.repository;

import com.securevault.entity.PasswordHistory;
import com.securevault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository
        extends JpaRepository<PasswordHistory, Long> {

    List<PasswordHistory> findByUserOrderByCreatedAtDesc(User user);
}
