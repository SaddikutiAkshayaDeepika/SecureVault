package com.securevault.repository;

import com.securevault.entity.CredentialShare;
import com.securevault.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CredentialShareRepository
        extends JpaRepository<CredentialShare, Long> {

    List<CredentialShare> findByRecipient(User recipient);

    List<CredentialShare> findByOwner(User owner);

    @Modifying
    @Query(
        value = "DELETE FROM credential_shares WHERE credential_id = :credentialId",
        nativeQuery = true
    )
    int deleteByCredentialId(
            @Param("credentialId") Long credentialId);
}