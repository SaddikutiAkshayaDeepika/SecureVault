package com.securevault.controller;

import com.securevault.dto.CredentialShareResponse;
import com.securevault.entity.CredentialShare;
import com.securevault.entity.User;
import com.securevault.repository.UserRepository;
import com.securevault.service.CredentialShareService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vault/shares")
public class CredentialShareController {

    private final CredentialShareService shareService;
    private final UserRepository userRepository;

    public CredentialShareController(
            CredentialShareService shareService,
            UserRepository userRepository) {

        this.shareService = shareService;
        this.userRepository = userRepository;
    }

    // CREATE SHARE
    @PostMapping
    public ResponseEntity<?> createShare(
            @RequestParam Long credentialId,
            @RequestParam String recipientEmail,
            @RequestParam String expiresAt,
            @RequestParam String permission,
            @RequestAttribute("email") String ownerEmail) {

        try {

            User owner =
                    userRepository.findByEmail(ownerEmail)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found"));

            LocalDateTime expiry =
                    LocalDateTime.parse(expiresAt);

            CredentialShare share =
                    shareService.createShare(
                            credentialId,
                            recipientEmail,
                            expiry,
                            permission,
                            owner);

            CredentialShareResponse response =
                    new CredentialShareResponse(
                            share.getId(),
                            share.getCredential().getId(),
                            share.getCredential().getTitle(),
                            share.getCredential().getUsername(),
                            share.getExpiresAt(),
                            share.isActive(),
                            share.getPermission()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // GET SHARES RECEIVED BY CURRENT USER
    @GetMapping
    public ResponseEntity<?> getReceivedShares(
            @RequestAttribute("email") String recipientEmail) {

        try {

            User recipient =
                    userRepository.findByEmail(recipientEmail)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found"));

            List<CredentialShare> shares =
                    shareService.getReceivedShares(recipient);

            List<CredentialShareResponse> response =
                    shares.stream()
                            .map(share -> new CredentialShareResponse(
                                    share.getId(),
                                    share.getCredential().getId(),
                                    share.getCredential().getTitle(),
                                    share.getCredential().getUsername(),
                                    share.getExpiresAt(),
                                    share.isActive(),
                                    share.getPermission()
                            ))
                            .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // GET SHARES CREATED BY OWNER
    @GetMapping("/owned")
    public ResponseEntity<?> getOwnedShares(
            @RequestAttribute("email") String ownerEmail) {

        try {

            User owner =
                    userRepository.findByEmail(ownerEmail)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found"));

            List<CredentialShare> shares =
                    shareService.getOwnedShares(owner);

            List<CredentialShareResponse> response =
                    shares.stream()
                            .map(share -> new CredentialShareResponse(
                                    share.getId(),
                                    share.getCredential().getId(),
                                    share.getCredential().getTitle(),
                                    share.getCredential().getUsername(),
                                    share.getExpiresAt(),
                                    share.isActive(),
                                    share.getPermission()
                            ))
                            .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    // REVOKE SHARE
    @DeleteMapping("/{shareId}")
    public ResponseEntity<?> revokeShare(
            @PathVariable Long shareId,
            @RequestAttribute("email") String ownerEmail) {

        try {

            User owner =
                    userRepository.findByEmail(ownerEmail)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found"));

            shareService.revokeShare(
                    shareId,
                    owner);

            return ResponseEntity.ok(
                    "Share revoked successfully");

        } catch (RuntimeException e) {

            return ResponseEntity.status(403)
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }
}