package com.securevault.service;

import com.securevault.entity.Credential;
import com.securevault.entity.CredentialShare;
import com.securevault.entity.User;
import com.securevault.repository.CredentialRepository;
import com.securevault.repository.CredentialShareRepository;
import com.securevault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialShareServiceTest {

    @Mock
    private CredentialShareRepository shareRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityEventService securityEventService;

    @InjectMocks
    private CredentialShareService credentialShareService;


    // 1. Create share successfully
    @Test
    void createShare_shouldCreateShareSuccessfully() {

        User owner = createUser(1L, "owner@gmail.com");
        User recipient = createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        LocalDateTime expiry =
                LocalDateTime.now().plusDays(1);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        when(userRepository.findByEmail("recipient@gmail.com"))
                .thenReturn(Optional.of(recipient));

        when(shareRepository.save(any(CredentialShare.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CredentialShare result =
                credentialShareService.createShare(
                        10L,
                        "recipient@gmail.com",
                        expiry,
                        "VIEW_ONLY",
                        owner
                );

        assertNotNull(result);
        assertEquals(credential, result.getCredential());
        assertEquals(owner, result.getOwner());
        assertEquals(recipient, result.getRecipient());
        assertEquals("VIEW_ONLY", result.getPermission());
        assertTrue(result.isActive());

        verify(shareRepository).save(any(CredentialShare.class));

        verify(securityEventService).recordEvent(
                "owner@gmail.com",
                "CREDENTIAL_SHARED",
                "N/A",
                "N/A"
        );
    }


    // 2. Create share - credential not found
    @Test
    void createShare_shouldRejectMissingCredential() {

        User owner =
                createUser(1L, "owner@gmail.com");

        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        999L,
                        "recipient@gmail.com",
                        LocalDateTime.now().plusDays(1),
                        "VIEW_ONLY",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 3. Only owner can share
    @Test
    void createShare_shouldRejectNonOwner() {

        User owner =
                createUser(1L, "owner@gmail.com");

        User otherUser =
                createUser(2L, "other@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "recipient@gmail.com",
                        LocalDateTime.now().plusDays(1),
                        "VIEW_ONLY",
                        otherUser
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 4. Expiry is required
    @Test
    void createShare_shouldRejectNullExpiry() {

        User owner =
                createUser(1L, "owner@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "recipient@gmail.com",
                        null,
                        "VIEW_ONLY",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 5. Expiry must be in future
    @Test
    void createShare_shouldRejectPastExpiry() {

        User owner =
                createUser(1L, "owner@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        LocalDateTime past =
                LocalDateTime.now().minusDays(1);

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "recipient@gmail.com",
                        past,
                        "VIEW_ONLY",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 6. Recipient must exist
    @Test
    void createShare_shouldRejectMissingRecipient() {

        User owner =
                createUser(1L, "owner@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        when(userRepository.findByEmail("missing@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "missing@gmail.com",
                        LocalDateTime.now().plusDays(1),
                        "VIEW_ONLY",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 7. Owner cannot share with themselves
    @Test
    void createShare_shouldRejectSelfSharing() {

        User owner =
                createUser(1L, "owner@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        when(userRepository.findByEmail("owner@gmail.com"))
                .thenReturn(Optional.of(owner));

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "owner@gmail.com",
                        LocalDateTime.now().plusDays(1),
                        "VIEW_ONLY",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 8. Permission is required
    @Test
    void createShare_shouldRejectMissingPermission() {

        User owner =
                createUser(1L, "owner@gmail.com");

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        when(userRepository.findByEmail("recipient@gmail.com"))
                .thenReturn(Optional.of(recipient));

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "recipient@gmail.com",
                        LocalDateTime.now().plusDays(1),
                        "",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 9. Invalid permission
    @Test
    void createShare_shouldRejectInvalidPermission() {

        User owner =
                createUser(1L, "owner@gmail.com");

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        when(userRepository.findByEmail("recipient@gmail.com"))
                .thenReturn(Optional.of(recipient));

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.createShare(
                        10L,
                        "recipient@gmail.com",
                        LocalDateTime.now().plusDays(1),
                        "INVALID",
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 10. Get received shares
    @Test
    void getReceivedShares_shouldReturnShares() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        CredentialShare share =
                createShareObject(
                        recipient,
                        "VIEW_ONLY",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        List<CredentialShare> result =
                credentialShareService.getReceivedShares(recipient);

        assertEquals(1, result.size());
        assertEquals("VIEW_ONLY", result.get(0).getPermission());

        verify(shareRepository, never()).save(any());
    }


    // 11. Expired received share becomes inactive
    @Test
    void getReceivedShares_shouldDeactivateExpiredShare() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        CredentialShare share =
                createShareObject(
                        recipient,
                        "VIEW_ONLY",
                        LocalDateTime.now().minusDays(1),
                        true
                );

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        List<CredentialShare> result =
                credentialShareService.getReceivedShares(recipient);

        assertFalse(result.get(0).isActive());

        verify(shareRepository).save(share);
    }


    // 12. Get owned shares
    @Test
    void getOwnedShares_shouldReturnShares() {

        User owner =
                createUser(1L, "owner@gmail.com");

        CredentialShare share =
                createShareObject(
                        owner,
                        "EDIT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        when(shareRepository.findByOwner(owner))
                .thenReturn(List.of(share));

        List<CredentialShare> result =
                credentialShareService.getOwnedShares(owner);

        assertEquals(1, result.size());
        assertEquals("EDIT", result.get(0).getPermission());
    }


    // 13. Expired owned share becomes inactive
    @Test
    void getOwnedShares_shouldDeactivateExpiredShare() {

        User owner =
                createUser(1L, "owner@gmail.com");

        CredentialShare share =
                createShareObject(
                        owner,
                        "EDIT",
                        LocalDateTime.now().minusDays(1),
                        true
                );

        when(shareRepository.findByOwner(owner))
                .thenReturn(List.of(share));

        List<CredentialShare> result =
                credentialShareService.getOwnedShares(owner);

        assertFalse(result.get(0).isActive());

        verify(shareRepository).save(share);
    }


    // 14. VIEW_ONLY permission works for viewing
    @Test
    void hasPermission_shouldAllowViewOnlyForViewing() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(10L, createUser(
                        1L,
                        "owner@gmail.com"
                ));

        CredentialShare share =
                createShareObject(
                        recipient,
                        "VIEW_ONLY",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        boolean result =
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "VIEW_ONLY"
                );

        assertTrue(result);
    }


    // 15. VIEW_ONLY cannot edit
    @Test
    void hasPermission_shouldRejectEditForViewOnly() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "VIEW_ONLY",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        boolean result =
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "EDIT"
                );

        assertFalse(result);
    }


    // 16. EDIT allows viewing
    @Test
    void hasPermission_shouldAllowViewForEdit() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "EDIT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        boolean result =
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "VIEW_ONLY"
                );

        assertTrue(result);
    }


    // 17. EDIT allows editing
    @Test
    void hasPermission_shouldAllowEditForEdit() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "EDIT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        boolean result =
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "EDIT"
                );

        assertTrue(result);
    }


    // 18. FULL_MANAGEMENT allows everything
    @Test
    void hasPermission_shouldAllowFullManagement() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "FULL_MANAGEMENT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        assertTrue(
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "VIEW_ONLY"
                )
        );

        assertTrue(
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "EDIT"
                )
        );

        assertTrue(
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "FULL_MANAGEMENT"
                )
        );
    }


    // 19. Inactive share is rejected
    @Test
    void hasPermission_shouldRejectInactiveShare() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "FULL_MANAGEMENT",
                        LocalDateTime.now().plusDays(1),
                        false
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        assertFalse(
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "VIEW_ONLY"
                )
        );
    }


    // 20. Expired share is rejected
    @Test
    void hasPermission_shouldRejectExpiredShare() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "FULL_MANAGEMENT",
                        LocalDateTime.now().minusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        assertFalse(
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "VIEW_ONLY"
                )
        );

        verify(shareRepository).save(share);
        assertFalse(share.isActive());
    }


    // 21. Wrong credential ID is rejected
    @Test
    void hasPermission_shouldRejectWrongCredential() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        "FULL_MANAGEMENT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        assertFalse(
                credentialShareService.hasPermission(
                        999L,
                        recipient,
                        "VIEW_ONLY"
                )
        );
    }


    // 22. Revoke share successfully
    @Test
    void revokeShare_shouldDeactivateShare() {

        User owner =
                createUser(1L, "owner@gmail.com");

        CredentialShare share =
                createShareObject(
                        owner,
                        "EDIT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        when(shareRepository.findById(1L))
                .thenReturn(Optional.of(share));

        credentialShareService.revokeShare(
                1L,
                owner
        );

        assertFalse(share.isActive());

        verify(shareRepository).save(share);

        verify(securityEventService).recordEvent(
                "owner@gmail.com",
                "SHARE_REVOKED",
                "N/A",
                "N/A"
        );
    }


    // 23. Revoke - share not found
    @Test
    void revokeShare_shouldRejectMissingShare() {

        User owner =
                createUser(1L, "owner@gmail.com");

        when(shareRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.revokeShare(
                        999L,
                        owner
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 24. Only owner can revoke
    @Test
    void revokeShare_shouldRejectNonOwner() {

        User owner =
                createUser(1L, "owner@gmail.com");

        User otherUser =
                createUser(2L, "other@gmail.com");

        CredentialShare share =
                createShareObject(
                        owner,
                        "EDIT",
                        LocalDateTime.now().plusDays(1),
                        true
                );

        when(shareRepository.findById(1L))
                .thenReturn(Optional.of(share));

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService.revokeShare(
                        1L,
                        otherUser
                )
        );

        verify(shareRepository, never()).save(any());
    }


    // 25. Delete shared credential successfully
    @Test
    void deleteSharedCredential_shouldDeleteCredential() {

        User owner =
                createUser(1L, "owner@gmail.com");

        Credential credential =
                createCredential(10L, owner);

        when(credentialRepository.findById(10L))
                .thenReturn(Optional.of(credential));

        credentialShareService.deleteSharedCredential(10L);

        verify(shareRepository)
                .deleteByCredentialId(10L);

        verify(shareRepository)
                .flush();

        verify(credentialRepository)
                .deleteCredentialById(10L);
    }


    // 26. Delete shared credential - not found
    @Test
    void deleteSharedCredential_shouldRejectMissingCredential() {

        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> credentialShareService
                        .deleteSharedCredential(999L)
        );

        verify(shareRepository, never())
                .deleteByCredentialId(anyLong());

        verify(credentialRepository, never())
                .deleteCredentialById(anyLong());
    }


    // 27. Null permission in old share is rejected
    @Test
    void hasPermission_shouldRejectMissingPermission() {

        User recipient =
                createUser(2L, "recipient@gmail.com");

        Credential credential =
                createCredential(
                        10L,
                        createUser(1L, "owner@gmail.com")
                );

        CredentialShare share =
                createShareObject(
                        recipient,
                        null,
                        LocalDateTime.now().plusDays(1),
                        true
                );

        share.setCredential(credential);

        when(shareRepository.findByRecipient(recipient))
                .thenReturn(List.of(share));

        assertFalse(
                credentialShareService.hasPermission(
                        10L,
                        recipient,
                        "VIEW_ONLY"
                )
        );
    }


    // Helper: create real User
    private User createUser(
            Long id,
            String email) {

        User user = new User();

        try {
            Field idField =
                    User.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(user, id);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        user.setEmail(email);

        return user;
    }


    // Helper: create Credential
    private Credential createCredential(
            Long id,
            User owner) {

        Credential credential =
                new Credential();

        try {
            Field idField =
                    Credential.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(credential, id);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        credential.setTitle("Google");
        credential.setUsername("testuser");
        credential.setPassword("encrypted");
        credential.setUser(owner);

        return credential;
    }


    // Helper: create CredentialShare
    private CredentialShare createShareObject(
            User user,
            String permission,
            LocalDateTime expiresAt,
            boolean active) {

        CredentialShare share =
                new CredentialShare();

        share.setRecipient(user);
        share.setOwner(user);
        share.setPermission(permission);
        share.setExpiresAt(expiresAt);
        share.setActive(active);

        return share;
    }
}