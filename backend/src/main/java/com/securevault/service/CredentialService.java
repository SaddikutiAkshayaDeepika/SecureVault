package com.securevault.service;

import com.securevault.entity.Credential;
import com.securevault.entity.User;
import com.securevault.repository.CredentialRepository;
import com.securevault.security.EncryptionService;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final EncryptionService encryptionService;
    private final EntityManager entityManager;
    private final SecurityEventService securityEventService;

    public CredentialService(
            CredentialRepository credentialRepository,
            EncryptionService encryptionService,
            EntityManager entityManager,
            SecurityEventService securityEventService) {

        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
        this.entityManager = entityManager;
        this.securityEventService = securityEventService;
    }

    // ADD CREDENTIAL
    public Credential addCredential(
            Credential credential,
            User user) {

        if (credential.getTitle() == null ||
                credential.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Title is required");
        }

        if (credential.getUsername() == null ||
                credential.getUsername().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Username is required");
        }

        if (credential.getPassword() == null ||
                credential.getPassword().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Password is required");
        }

        String encryptedPassword =
                encryptionService.encrypt(
                        credential.getPassword());

        credential.setPassword(encryptedPassword);
        credential.setUser(user);

        Credential savedCredential =
                credentialRepository.save(credential);

        // AUDIT LOG
        securityEventService.recordEvent(
                user.getEmail(),
                "CREDENTIAL_ADDED",
                "N/A",
                "N/A"
        );

        return savedCredential;
    }

    // GET OWN CREDENTIALS
    public List<Credential> getUserCredentials(
            User user) {

        List<Credential> credentials =
                credentialRepository.findByUser(user);

        for (Credential credential : credentials) {

            try {

                String decryptedPassword =
                        encryptionService.decrypt(
                                credential.getPassword());

                credential.setPassword(decryptedPassword);

            } catch (Exception e) {

                System.out.println(
                        "DECRYPTION FAILED FOR CREDENTIAL ID: "
                                + credential.getId());

                // Do not expose the encrypted password
                credential.setPassword(null);
            }
        }

        return credentials;
    }

    // SEARCH OWN CREDENTIALS
    public List<Credential> searchCredentials(
            User user,
            String query) {

        if (query == null || query.trim().isEmpty()) {
            return getUserCredentials(user);
        }

        List<Credential> credentials =
                credentialRepository.searchCredentials(
                        user,
                        query.trim());

        for (Credential credential : credentials) {
            try {
                String decryptedPassword =
                        encryptionService.decrypt(
                                credential.getPassword());
                credential.setPassword(decryptedPassword);
            } catch (Exception e) {
                System.out.println(
                        "DECRYPTION FAILED FOR CREDENTIAL ID: "
                                + credential.getId());
                credential.setPassword(null);
            }
        }

        return credentials;
    }

    // GET SINGLE CREDENTIAL
    // IMPORTANT:
    // This returns the credential with its encrypted password.
    // Decryption happens only after permission is verified.
    public Credential getCredentialById(
            Long credentialId) {

        return credentialRepository.findById(credentialId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Credential not found"));
    }

    // DECRYPT PASSWORD AFTER AUTHORIZATION
    public Credential decryptCredential(
            Credential credential) {

        String decryptedPassword =
                encryptionService.decrypt(
                        credential.getPassword());

        credential.setPassword(decryptedPassword);

        return credential;
    }

    // UPDATE OWN CREDENTIAL
    public Credential updateCredential(
            Long credentialId,
            Credential updatedCredential,
            User user) {

        Credential existingCredential =
                credentialRepository.findById(credentialId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Credential not found"));

        if (!existingCredential.getUser()
                .getId()
                .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to update this credential");
        }

        Credential updated =
                updateCredentialData(
                        existingCredential,
                        updatedCredential);

        // AUDIT LOG
        securityEventService.recordEvent(
                user.getEmail(),
                "CREDENTIAL_UPDATED",
                "N/A",
                "N/A"
        );

        return updated;
    }

    // UPDATE SHARED CREDENTIAL
    public Credential updateSharedCredential(
            Long credentialId,
            Credential updatedCredential) {

        Credential existingCredential =
                credentialRepository.findById(credentialId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Credential not found"));

        Credential updated =
                updateCredentialData(
                        existingCredential,
                        updatedCredential);

        // AUDIT LOG
        if (existingCredential.getUser() != null) {

            securityEventService.recordEvent(
                    existingCredential.getUser().getEmail(),
                    "SHARED_CREDENTIAL_UPDATED",
                    "N/A",
                    "N/A"
            );
        }

        return updated;
    }

    // COMMON UPDATE LOGIC
    private Credential updateCredentialData(
            Credential existingCredential,
            Credential updatedCredential) {

        if (updatedCredential.getTitle() == null ||
                updatedCredential.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Title is required");
        }

        if (updatedCredential.getUsername() == null ||
                updatedCredential.getUsername().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Username is required");
        }

        if (updatedCredential.getPassword() == null ||
                updatedCredential.getPassword().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Password is required");
        }

        existingCredential.setTitle(
                updatedCredential.getTitle());

        existingCredential.setUsername(
                updatedCredential.getUsername());

        existingCredential.setCategory(
                updatedCredential.getCategory());

        existingCredential.setCredentialType(
                updatedCredential.getCredentialType());

        existingCredential.setFavorite(
                updatedCredential.isFavorite());

        String encryptedPassword =
                encryptionService.encrypt(
                        updatedCredential.getPassword());

        existingCredential.setPassword(
                encryptedPassword);

        return credentialRepository.save(
                existingCredential);
    }

    // DELETE OWN CREDENTIAL
    @Transactional
    public void deleteCredential(
            Long credentialId,
            User user) {

        Credential credential =
                credentialRepository.findById(credentialId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Credential not found"));

        if (!credential.getUser()
                .getId()
                .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to delete this credential");
        }

        // Remove related share rows first
        entityManager.createNativeQuery(
                "DELETE FROM credential_shares " +
                "WHERE credential_id = :credentialId")
                .setParameter(
                        "credentialId",
                        credentialId)
                .executeUpdate();

        // Remove managed Hibernate entities
        entityManager.clear();

        // Delete credential
        entityManager.createNativeQuery(
                "DELETE FROM credentials " +
                "WHERE id = :credentialId")
                .setParameter(
                        "credentialId",
                        credentialId)
                .executeUpdate();

        entityManager.flush();

        // AUDIT LOG
        securityEventService.recordEvent(
                user.getEmail(),
                "CREDENTIAL_DELETED",
                "N/A",
                "N/A"
        );
    }

    // DELETE SHARED CREDENTIAL
    @Transactional
    public void deleteSharedCredential(
            Long credentialId) {

        Credential credential =
                credentialRepository.findById(credentialId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Credential not found"));

        String ownerEmail = null;

        if (credential.getUser() != null) {
            ownerEmail = credential.getUser().getEmail();
        }

        // Remove related share rows first
        entityManager.createNativeQuery(
                "DELETE FROM credential_shares " +
                "WHERE credential_id = :credentialId")
                .setParameter(
                        "credentialId",
                        credentialId)
                .executeUpdate();

        // Remove managed Hibernate entities
        entityManager.clear();

        // Delete credential directly
        entityManager.createNativeQuery(
                "DELETE FROM credentials " +
                "WHERE id = :credentialId")
                .setParameter(
                        "credentialId",
                        credentialId)
                .executeUpdate();

        entityManager.flush();

        // AUDIT LOG
        if (ownerEmail != null) {

            securityEventService.recordEvent(
                    ownerEmail,
                    "SHARED_CREDENTIAL_DELETED",
                    "N/A",
                    "N/A"
            );
        }
    }
}