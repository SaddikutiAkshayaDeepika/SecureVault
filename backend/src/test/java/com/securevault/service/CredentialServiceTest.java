package com.securevault.service;

import com.securevault.entity.Credential;
import com.securevault.entity.User;
import com.securevault.repository.CredentialRepository;
import com.securevault.security.EncryptionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredentialServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private Query query;

    @InjectMocks
    private CredentialService credentialService;


    // 1. Add credential successfully
    @Test
    void addCredential_shouldSaveCredential() {
        User user = createUser(1L, "test@gmail.com");
        Credential credential =
                createCredential("Google", "test@gmail.com", "password123");

        when(encryptionService.encrypt("password123"))
                .thenReturn("encrypted-password");

        when(credentialRepository.save(credential))
                .thenReturn(credential);

        Credential result =
                credentialService.addCredential(credential, user);

        assertNotNull(result);
        assertEquals("Google", result.getTitle());
        assertEquals("encrypted-password", result.getPassword());
        assertEquals(user, result.getUser());

        verify(credentialRepository).save(credential);

        verify(securityEventService).recordEvent(
                "test@gmail.com",
                "CREDENTIAL_ADDED",
                "N/A",
                "N/A"
        );
    }


    // 2. Add credential - missing title
    @Test
    void addCredential_shouldRejectMissingTitle() {
        User user = createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential("", "test@gmail.com", "password123");

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.addCredential(credential, user)
        );

        verify(credentialRepository, never()).save(any());
    }


    // 3. Add credential - missing username
    @Test
    void addCredential_shouldRejectMissingUsername() {
        User user = createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential("Google", "", "password123");

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.addCredential(credential, user)
        );

        verify(credentialRepository, never()).save(any());
    }


    // 4. Add credential - missing password
    @Test
    void addCredential_shouldRejectMissingPassword() {
        User user = createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential("Google", "test@gmail.com", "");

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.addCredential(credential, user)
        );

        verify(credentialRepository, never()).save(any());
    }


    // 5. Add credential - null title
    @Test
    void addCredential_shouldRejectNullTitle() {
        User user = createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential(null, "test@gmail.com", "password123");

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.addCredential(credential, user)
        );

        verify(credentialRepository, never()).save(any());
    }


    // 6. Get user's credentials
    @Test
    void getUserCredentials_shouldReturnDecryptedCredentials() {
        User user = createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential(
                        "Google",
                        "test@gmail.com",
                        "encrypted-password"
                );

        when(credentialRepository.findByUser(user))
                .thenReturn(List.of(credential));

        when(encryptionService.decrypt("encrypted-password"))
                .thenReturn("password123");

        List<Credential> result =
                credentialService.getUserCredentials(user);

        assertEquals(1, result.size());
        assertEquals(
                "password123",
                result.get(0).getPassword()
        );
    }


    // 7. Get credentials - decryption failure
    @Test
    void getUserCredentials_shouldHidePasswordWhenDecryptionFails() {
        User user = createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential(
                        "Google",
                        "test@gmail.com",
                        "encrypted-password"
                );

        when(credentialRepository.findByUser(user))
                .thenReturn(List.of(credential));

        when(encryptionService.decrypt("encrypted-password"))
                .thenThrow(new RuntimeException("Decryption failed"));

        List<Credential> result =
                credentialService.getUserCredentials(user);

        assertNull(result.get(0).getPassword());
    }


    // 8. Get credentials - empty list
    @Test
    void getUserCredentials_shouldReturnEmptyList() {
        User user = createUser(1L, "test@gmail.com");

        when(credentialRepository.findByUser(user))
                .thenReturn(List.of());

        List<Credential> result =
                credentialService.getUserCredentials(user);

        assertTrue(result.isEmpty());
    }


    // 9. Get credential by ID
    @Test
    void getCredentialById_shouldReturnCredential() {
        Credential credential =
                createCredential(
                        "Google",
                        "test@gmail.com",
                        "encrypted"
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(credential));

        Credential result =
                credentialService.getCredentialById(1L);

        assertEquals("Google", result.getTitle());
    }


    // 10. Get credential by ID - not found
    @Test
    void getCredentialById_shouldRejectMissingCredential() {
        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.getCredentialById(999L)
        );
    }


    // 11. Decrypt credential
    @Test
    void decryptCredential_shouldDecryptPassword() {
        Credential credential =
                createCredential(
                        "Google",
                        "test@gmail.com",
                        "encrypted"
                );

        when(encryptionService.decrypt("encrypted"))
                .thenReturn("password123");

        Credential result =
                credentialService.decryptCredential(credential);

        assertEquals("password123", result.getPassword());
    }


    // 12. Update own credential
    @Test
    void updateCredential_shouldUpdateCredential() {
        User user = createUser(1L, "test@gmail.com");

        Credential existing =
                createCredential(
                        "Google",
                        "olduser",
                        "oldencrypted"
                );

        existing.setUser(user);

        Credential updated =
                createCredential(
                        "Google Updated",
                        "newuser",
                        "newpassword"
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(encryptionService.encrypt("newpassword"))
                .thenReturn("newencrypted");

        when(credentialRepository.save(existing))
                .thenReturn(existing);

        Credential result =
                credentialService.updateCredential(
                        1L,
                        updated,
                        user
                );

        assertEquals("Google Updated", result.getTitle());
        assertEquals("newuser", result.getUsername());
        assertEquals("newencrypted", result.getPassword());

        verify(securityEventService).recordEvent(
                "test@gmail.com",
                "CREDENTIAL_UPDATED",
                "N/A",
                "N/A"
        );
    }


    // 13. Update credential - not owner
    @Test
    void updateCredential_shouldRejectNonOwner() {
        User owner =
                createUser(1L, "owner@gmail.com");

        User otherUser =
                createUser(2L, "other@gmail.com");

        Credential existing =
                createCredential(
                        "Google",
                        "owner",
                        "encrypted"
                );

        existing.setUser(owner);

        Credential updated =
                createCredential(
                        "Google Updated",
                        "other",
                        "password"
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.updateCredential(
                        1L,
                        updated,
                        otherUser
                )
        );

        verify(credentialRepository, never()).save(any());
    }


    // 14. Update credential - not found
    @Test
    void updateCredential_shouldRejectMissingCredential() {
        User user =
                createUser(1L, "test@gmail.com");

        Credential updated =
                createCredential(
                        "Google",
                        "user",
                        "password"
                );

        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.updateCredential(
                        999L,
                        updated,
                        user
                )
        );
    }


    // 15. Update credential - missing title
    @Test
    void updateCredential_shouldRejectMissingTitle() {
        User user =
                createUser(1L, "test@gmail.com");

        Credential existing =
                createCredential(
                        "Google",
                        "user",
                        "encrypted"
                );

        existing.setUser(user);

        Credential updated =
                createCredential(
                        "",
                        "newuser",
                        "password"
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.updateCredential(
                        1L,
                        updated,
                        user
                )
        );
    }


    // 16. Update credential - missing username
    @Test
    void updateCredential_shouldRejectMissingUsername() {
        User user =
                createUser(1L, "test@gmail.com");

        Credential existing =
                createCredential(
                        "Google",
                        "user",
                        "encrypted"
                );

        existing.setUser(user);

        Credential updated =
                createCredential(
                        "Google",
                        "",
                        "password"
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.updateCredential(
                        1L,
                        updated,
                        user
                )
        );
    }


    // 17. Update credential - missing password
    @Test
    void updateCredential_shouldRejectMissingPassword() {
        User user =
                createUser(1L, "test@gmail.com");

        Credential existing =
                createCredential(
                        "Google",
                        "user",
                        "encrypted"
                );

        existing.setUser(user);

        Credential updated =
                createCredential(
                        "Google",
                        "newuser",
                        ""
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.updateCredential(
                        1L,
                        updated,
                        user
                )
        );
    }


    // 18. Delete credential - not owner
    @Test
    void deleteCredential_shouldRejectNonOwner() {
        User owner =
                createUser(1L, "owner@gmail.com");

        User otherUser =
                createUser(2L, "other@gmail.com");

        Credential credential =
                createCredential(
                        "Google",
                        "owner",
                        "encrypted"
                );

        credential.setUser(owner);

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(credential));

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.deleteCredential(
                        1L,
                        otherUser
                )
        );

        verify(entityManager, never())
                .createNativeQuery(anyString());
    }


    // 19. Delete credential - not found
    @Test
    void deleteCredential_shouldRejectMissingCredential() {
        User user =
                createUser(1L, "test@gmail.com");

        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.deleteCredential(
                        999L,
                        user
                )
        );
    }


    // 20. Delete credential - success
    @Test
    void deleteCredential_shouldDeleteCredential() {
        User user =
                createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential(
                        "Google",
                        "user",
                        "encrypted"
                );

        credential.setUser(user);

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(credential));

        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(query);

        when(query.setParameter(anyString(), any()))
                .thenReturn(query);

        when(query.executeUpdate())
                .thenReturn(1);

        credentialService.deleteCredential(
                1L,
                user
        );

        verify(entityManager, times(2))
                .createNativeQuery(anyString());

        verify(securityEventService).recordEvent(
                "test@gmail.com",
                "CREDENTIAL_DELETED",
                "N/A",
                "N/A"
        );
    }


    // 21. Update shared credential
    @Test
    void updateSharedCredential_shouldUpdateCredential() {
        User owner =
                createUser(1L, "owner@gmail.com");

        Credential existing =
                createCredential(
                        "Google",
                        "olduser",
                        "encrypted"
                );

        existing.setUser(owner);

        Credential updated =
                createCredential(
                        "Google Updated",
                        "newuser",
                        "newpassword"
                );

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(encryptionService.encrypt("newpassword"))
                .thenReturn("newencrypted");

        when(credentialRepository.save(existing))
                .thenReturn(existing);

        Credential result =
                credentialService.updateSharedCredential(
                        1L,
                        updated
                );

        assertEquals(
                "Google Updated",
                result.getTitle()
        );

        assertEquals(
                "newencrypted",
                result.getPassword()
        );

        verify(securityEventService).recordEvent(
                "owner@gmail.com",
                "SHARED_CREDENTIAL_UPDATED",
                "N/A",
                "N/A"
        );
    }


    // 22. Shared credential - not found
    @Test
    void updateSharedCredential_shouldRejectMissingCredential() {
        Credential updated =
                createCredential(
                        "Google",
                        "user",
                        "password"
                );

        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.updateSharedCredential(
                        999L,
                        updated
                )
        );
    }


    // 23. Delete shared credential
    @Test
    void deleteSharedCredential_shouldDeleteCredential() {
        User owner =
                createUser(1L, "owner@gmail.com");

        Credential credential =
                createCredential(
                        "Google",
                        "user",
                        "encrypted"
                );

        credential.setUser(owner);

        when(credentialRepository.findById(1L))
                .thenReturn(Optional.of(credential));

        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(query);

        when(query.setParameter(anyString(), any()))
                .thenReturn(query);

        when(query.executeUpdate())
                .thenReturn(1);

        credentialService.deleteSharedCredential(1L);

        verify(entityManager, times(2))
                .createNativeQuery(anyString());

        verify(securityEventService).recordEvent(
                "owner@gmail.com",
                "SHARED_CREDENTIAL_DELETED",
                "N/A",
                "N/A"
        );
    }


    // 24. Shared credential - not found
    @Test
    void deleteSharedCredential_shouldRejectMissingCredential() {
        when(credentialRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> credentialService.deleteSharedCredential(999L)
        );
    }


    // 25. Add credential should encrypt password before saving
    @Test
    void addCredential_shouldEncryptPasswordBeforeSaving() {
        User user =
                createUser(1L, "test@gmail.com");

        Credential credential =
                createCredential(
                        "GitHub",
                        "developer",
                        "secret123"
                );

        when(encryptionService.encrypt("secret123"))
                .thenReturn("encrypted-secret");

        when(credentialRepository.save(credential))
                .thenReturn(credential);

        credentialService.addCredential(
                credential,
                user
        );

        verify(encryptionService)
                .encrypt("secret123");

        assertEquals(
                "encrypted-secret",
                credential.getPassword()
        );
    }


    // Helper method for creating a real User
    private User createUser(Long id, String email) {
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


    // Helper method for creating Credential
    private Credential createCredential(
            String title,
            String username,
            String password) {

        Credential credential =
                new Credential();

        credential.setTitle(title);
        credential.setUsername(username);
        credential.setPassword(password);

        return credential;
    }
}