package com.securevault.dto;

import java.time.LocalDateTime;

public class CredentialShareResponse {

    private Long id;
    private Long credentialId;
    private String title;
    private String username;
    private LocalDateTime expiresAt;
    private boolean active;
    private String permission;

    public CredentialShareResponse(
            Long id,
            Long credentialId,
            String title,
            String username,
            LocalDateTime expiresAt,
            boolean active,
            String permission) {

        this.id = id;
        this.credentialId = credentialId;
        this.title = title;
        this.username = username;
        this.expiresAt = expiresAt;
        this.active = active;
        this.permission = permission;
    }

    public Long getId() {
        return id;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public String getTitle() {
        return title;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getPermission() {
        return permission;
    }
}