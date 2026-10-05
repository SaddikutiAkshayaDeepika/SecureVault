package com.securevault.repository;

import com.securevault.entity.Credential;
import com.securevault.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CredentialRepository
        extends JpaRepository<Credential, Long> {

    List<Credential> findByUser(User user);

    @Query("SELECT c FROM Credential c WHERE c.user = :user " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Credential> searchCredentials(
            @Param("user") User user,
            @Param("query") String query);

    @Modifying
    @Query(
        value = "DELETE FROM credentials WHERE id = :credentialId",
        nativeQuery = true
    )
    int deleteCredentialById(
            @Param("credentialId") Long credentialId);
}