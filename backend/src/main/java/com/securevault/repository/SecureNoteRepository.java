package com.securevault.repository;

import com.securevault.entity.SecureNote;
import com.securevault.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecureNoteRepository
        extends JpaRepository<SecureNote, Long> {

    List<SecureNote> findByUser(User user);
}
