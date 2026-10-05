package com.securevault.repository;

import com.securevault.entity.Mfa;
import com.securevault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaRepository extends JpaRepository<Mfa, Long> {

    Optional<Mfa> findByUser(User user);
}