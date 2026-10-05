package com.securevault.service;

import com.securevault.entity.Mfa;
import com.securevault.entity.Role;
import com.securevault.entity.User;
import com.securevault.repository.MfaRepository;
import com.securevault.repository.UserRepository;
import com.securevault.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private MfaRepository mfaRepository;

    @Mock
    private SessionService sessionService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;


    // 1. Register new user
    @Test
    void registerUser_shouldRegisterNewUser() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("Password123");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(user);

        assertNotNull(result);
        assertEquals("test@gmail.com", result.getEmail());
        assertEquals(Role.USER, result.getRole());
        assertNotEquals("Password123", result.getPassword());

        verify(userRepository).save(user);
    }


    // 2. Duplicate email
    @Test
    void registerUser_shouldRejectDuplicateEmail() {
        User existingUser = new User();
        existingUser.setEmail("test@gmail.com");

        User newUser = new User();
        newUser.setEmail("test@gmail.com");
        newUser.setPassword("Password123");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(newUser)
        );

        verify(userRepository, never()).save(any(User.class));
    }


    // 3. Register user with existing role
    @Test
    void registerUser_shouldKeepExistingRole() {
        User user = new User();
        user.setEmail("admin@gmail.com");
        user.setPassword("Password123");
        user.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@gmail.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerUser(user);

        assertEquals(Role.ADMIN, result.getRole());
        assertNotEquals("Password123", result.getPassword());
    }


    // 4. Successful login
    @Test
    void loginUser_shouldLoginSuccessfully() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(securityEventService.analyzeLogin(anyString(), anyString(), anyString()))
                .thenReturn("LOW");

        when(mfaRepository.findByUser(user))
                .thenReturn(Optional.empty());

        when(sessionService.createSession(anyString(), anyString(), anyString()))
                .thenReturn("session-123");

        when(jwtService.generateToken(
                eq("test@gmail.com"),
                eq(Role.USER.name()),
                eq("session-123")
        )).thenReturn("jwt-token");

        // Use a known BCrypt password for "password"
        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("password"));

        String token = userService.loginUser(
                "test@gmail.com",
                "password",
                "127.0.0.1",
                "Chrome"
        );

        assertEquals("jwt-token", token);

        verify(securityEventService)
                .recordLogin("test@gmail.com", "SUCCESS", "127.0.0.1", "Chrome");

        verify(sessionService)
                .createSession("test@gmail.com", "Chrome", "127.0.0.1");
    }


    // 5. Wrong password
    @Test
    void loginUser_shouldRejectWrongPassword() {
        User user = new User();
        user.setEmail("test@gmail.com");

        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("correctPassword"));

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        String token = userService.loginUser(
                "test@gmail.com",
                "wrongPassword",
                "127.0.0.1",
                "Chrome"
        );

        assertNull(token);

        verify(securityEventService)
                .recordLogin("test@gmail.com", "FAILED", "127.0.0.1", "Chrome");

        verify(sessionService, never())
                .createSession(anyString(), anyString(), anyString());
    }


    // 6. Unknown user login
    @Test
    void loginUser_shouldRejectUnknownUser() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        String token = userService.loginUser(
                "unknown@gmail.com",
                "password",
                "127.0.0.1",
                "Chrome"
        );

        assertNull(token);

        verify(securityEventService)
                .recordLogin("unknown@gmail.com", "FAILED", "127.0.0.1", "Chrome");
    }


    // 7. MFA login
    @Test
    void loginUser_shouldReturnMfaTokenWhenMfaEnabled() {
        User user = new User();
        user.setEmail("test@gmail.com");

        user.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode("password"));

        Mfa mfa = mock(Mfa.class);
        when(mfa.isEnabled()).thenReturn(true);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(securityEventService.analyzeLogin(anyString(), anyString(), anyString()))
                .thenReturn("LOW");

        when(mfaRepository.findByUser(user))
                .thenReturn(Optional.of(mfa));

        when(jwtService.generateMfaToken("test@gmail.com"))
                .thenReturn("mfa-token");

        String token = userService.loginUser(
                "test@gmail.com",
                "password",
                "127.0.0.1",
                "Chrome"
        );

        assertEquals("mfa-token", token);

        verify(jwtService).generateMfaToken("test@gmail.com");

        verify(sessionService, never())
                .createSession(anyString(), anyString(), anyString());
    }


    // 8. Failed login event
    @Test
    void loginUser_shouldRecordFailedLogin() {
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        userService.loginUser(
                "test@gmail.com",
                "wrongPassword",
                "192.168.1.1",
                "Safari"
        );

        verify(securityEventService)
                .recordLogin(
                        "test@gmail.com",
                        "FAILED",
                        "192.168.1.1",
                        "Safari"
                );
    }


    // 9. Complete MFA login
    @Test
    void completeMfaLogin_shouldCreateSessionAndToken() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(sessionService.createSession(
                "test@gmail.com",
                "Chrome",
                "127.0.0.1"
        )).thenReturn("session-456");

        when(jwtService.generateToken(
                "test@gmail.com",
                Role.USER.name(),
                "session-456"
        )).thenReturn("final-jwt");

        String token = userService.completeMfaLogin(
                "test@gmail.com",
                "127.0.0.1",
                "Chrome"
        );

        assertEquals("final-jwt", token);
    }


    // 10. Complete MFA login - user not found
    @Test
    void completeMfaLogin_shouldRejectUnknownUser() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.completeMfaLogin(
                        "unknown@gmail.com",
                        "127.0.0.1",
                        "Chrome"
                )
        );
    }


    // 11. Password reset token - empty email
    @Test
    void createPasswordResetToken_shouldReturnNullForEmptyEmail() {
        String token = userService.createPasswordResetToken("");

        assertNull(token);

        verify(userRepository, never())
                .findByEmail(anyString());
    }


    // 12. Password reset token - unknown email
    @Test
    void createPasswordResetToken_shouldReturnNullForUnknownUser() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        String token = userService.createPasswordResetToken(
                "unknown@gmail.com"
        );

        assertNull(token);

        verify(userRepository, never())
                .save(any(User.class));
    }


    // 13. Password reset token - success
    @Test
    void createPasswordResetToken_shouldCreateToken() {
        User user = new User();
        user.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        String token = userService.createPasswordResetToken(
                "test@gmail.com"
        );

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertNotNull(user.getPasswordResetTokenHash());
        assertNotNull(user.getPasswordResetTokenExpiry());

        verify(userRepository).save(user);
    }


    // 14. Get user by email
    @Test
    void getUserByEmail_shouldReturnUser() {
        User user = new User();
        user.setEmail("test@gmail.com");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("test@gmail.com");

        assertEquals("test@gmail.com", result.getEmail());
    }


    // 15. Get user by email - not found
    @Test
    void getUserByEmail_shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                Exception.class,
                () -> userService.getUserByEmail("unknown@gmail.com")
        );
    }


    // 16. Update user role
    @Test
    void updateUserRole_shouldUpdateRole() {
        User user = new User();
        user.setEmail("test@gmail.com");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.updateUserRole(
                "test@gmail.com",
                Role.ADMIN
        );

        assertEquals(Role.ADMIN, result.getRole());

        verify(userRepository).save(user);
    }


    // 17. Update role - unknown user
    @Test
    void updateUserRole_shouldRejectUnknownUser() {
        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserRole(
                        "unknown@gmail.com",
                        Role.ADMIN
                )
        );

        verify(userRepository, never())
                .save(any(User.class));
    }


    // 18. Reset password - empty token
    @Test
    void resetPassword_shouldRejectEmptyToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(
                        "",
                        "NewPassword123"
                )
        );
    }


    // 19. Reset password - empty password
    @Test
    void resetPassword_shouldRejectEmptyPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(
                        "valid-token",
                        ""
                )
        );
    }


    // 20. Reset password - invalid token
    @Test
    void resetPassword_shouldRejectInvalidToken() {
        when(userRepository.findByPasswordResetTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(
                        "invalid-token",
                        "NewPassword123"
                )
        );
    }
}