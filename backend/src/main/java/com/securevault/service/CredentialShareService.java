
package com.securevault.service;

import com.securevault.entity.Credential;
import com.securevault.entity.CredentialShare;
import com.securevault.entity.User;
import com.securevault.repository.CredentialRepository;
import com.securevault.repository.CredentialShareRepository;
import com.securevault.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CredentialShareService {

    private final CredentialShareRepository shareRepository;
    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final SecurityEventService securityEventService;

    public CredentialShareService(
            CredentialShareRepository shareRepository,
            CredentialRepository credentialRepository,
            UserRepository userRepository,
            SecurityEventService securityEventService) {

        this.shareRepository = shareRepository;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.securityEventService = securityEventService;
    }

    // CREATE NEW SHARE
    public CredentialShare createShare(
            Long credentialId,
            String recipientEmail,
            LocalDateTime expiresAt,
            String permission,
            User owner) {

        Credential credential =
                credentialRepository.findById(credentialId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found"));

        // Only owner can share
        if (!credential.getUser()
                .getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You can only share your own credentials");
        }

        // Expiry must be in the future
        if (expiresAt == null ||
                !expiresAt.isAfter(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Expiry time must be in the future");
        }

        // Find recipient
        User recipient =
                userRepository.findByEmail(recipientEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Recipient user not found"));

        // Owner cannot share with themselves
        if (recipient.getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You cannot share a credential with yourself");
        }

        // Permission is required
        if (permission == null ||
                permission.trim().isEmpty()) {

            throw new RuntimeException(
                    "Permission is required");
        }

        // Validate permission
        if (!permission.equals("VIEW_ONLY") &&
                !permission.equals("EDIT") &&
                !permission.equals("FULL_MANAGEMENT")) {

            throw new RuntimeException(
                    "Invalid permission");
        }

        // Create share
        CredentialShare share =
                new CredentialShare();

        share.setCredential(credential);
        share.setOwner(owner);
        share.setRecipient(recipient);
        share.setExpiresAt(expiresAt);
        share.setPermission(permission);
        share.setActive(true);

        CredentialShare savedShare =
                shareRepository.save(share);

        // AUDIT LOG
        securityEventService.recordEvent(
                owner.getEmail(),
                "CREDENTIAL_SHARED",
                "N/A",
                "N/A"
        );

        return savedShare;
    }

    // GET SHARES RECEIVED BY USER
    public List<CredentialShare> getReceivedShares(
            User recipient) {

        List<CredentialShare> shares =
                shareRepository.findByRecipient(recipient);

        LocalDateTime now =
                LocalDateTime.now();

        // Automatically deactivate expired shares
        for (CredentialShare share : shares) {

            if (share.isActive() &&
                    !share.getExpiresAt()
                            .isAfter(now)) {

                share.setActive(false);
                shareRepository.save(share);
            }
        }

        return shares;
    }

    // GET SHARES CREATED BY OWNER
    public List<CredentialShare> getOwnedShares(
            User owner) {

        List<CredentialShare> shares =
                shareRepository.findByOwner(owner);

        LocalDateTime now =
                LocalDateTime.now();

        // Automatically deactivate expired shares
        for (CredentialShare share : shares) {

            if (share.isActive() &&
                    !share.getExpiresAt()
                            .isAfter(now)) {

                share.setActive(false);
                shareRepository.save(share);
            }
        }

        return shares;
    }

    // CHECK USER PERMISSION
    public boolean hasPermission(
            Long credentialId,
            User user,
            String requiredPermission) {

        List<CredentialShare> shares =
                shareRepository.findByRecipient(user);

        LocalDateTime now =
                LocalDateTime.now();

        for (CredentialShare share : shares) {

            // Check correct credential
            if (!share.getCredential()
                    .getId()
                    .equals(credentialId)) {

                continue;
            }

            // Ignore inactive shares
            if (!share.isActive()) {
                continue;
            }

            // Check expiry
            if (!share.getExpiresAt()
                    .isAfter(now)) {

                share.setActive(false);
                shareRepository.save(share);

                continue;
            }

            String permission =
                    share.getPermission();

            // Old shares may have no permission
            if (permission == null ||
                    permission.trim().isEmpty()) {

                continue;
            }

            // FULL_MANAGEMENT
            // can view, edit and delete
            if (permission.equals(
                    "FULL_MANAGEMENT")) {

                return true;
            }

            // EDIT
            // can view and edit
            if (permission.equals("EDIT")
                    && (requiredPermission.equals("EDIT")
                    || requiredPermission.equals(
                            "VIEW_ONLY"))) {

                return true;
            }

            // VIEW_ONLY
            // can only view
            if (permission.equals("VIEW_ONLY")
                    && requiredPermission.equals(
                            "VIEW_ONLY")) {

                return true;
            }
        }

        return false;
    }

    // REVOKE A SHARE
    @Transactional
    public void revokeShare(
            Long shareId,
            User owner) {

        CredentialShare share =
                shareRepository.findById(shareId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Share not found"));

        // Only the owner who created the share
        // can revoke it
        if (!share.getOwner()
                .getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "You are not allowed to revoke this share");
        }

        // Deactivate instead of deleting
        share.setActive(false);

        shareRepository.save(share);

        // AUDIT LOG
        securityEventService.recordEvent(
                owner.getEmail(),
                "SHARE_REVOKED",
                "N/A",
                "N/A"
        );
    }

    // DELETE SHARED CREDENTIAL
    // Used for FULL_MANAGEMENT permission
    @Transactional
    public void deleteSharedCredential(
            Long credentialId) {

        // Make sure credential exists
        Credential credential =
                credentialRepository.findById(credentialId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found"));

        String ownerEmail = null;

        if (credential.getUser() != null) {
            ownerEmail =
                    credential.getUser().getEmail();
        }

        // Delete all share rows directly
        shareRepository.deleteByCredentialId(
                credentialId);

        // Clear repository persistence state
        shareRepository.flush();

        // Delete credential
        credentialRepository.deleteCredentialById(
                credentialId);

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

