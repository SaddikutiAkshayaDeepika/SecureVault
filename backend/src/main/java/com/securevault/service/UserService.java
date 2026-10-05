package com.securevault.service;

import com.securevault.entity.Mfa;
import com.securevault.entity.Role;
import com.securevault.entity.User;
import com.securevault.repository.MfaRepository;
import com.securevault.repository.UserRepository;
import com.securevault.security.JwtService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final SecurityEventService securityEventService;
    private final MfaRepository mfaRepository;
    private final SessionService sessionService;
    private final NotificationService notificationService;

    private final PasswordHistoryService passwordHistoryService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    private final SecureRandom secureRandom =
            new SecureRandom();

    public UserService(
            UserRepository userRepository,
            JwtService jwtService,
            SecurityEventService securityEventService,
            MfaRepository mfaRepository,
            SessionService sessionService,
            NotificationService notificationService,
            PasswordHistoryService passwordHistoryService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.securityEventService = securityEventService;
        this.mfaRepository = mfaRepository;
        this.sessionService = sessionService;
        this.notificationService = notificationService;
        this.passwordHistoryService = passwordHistoryService;
    }

    // =========================
    // REGISTER
    // =========================

    public User registerUser(User user) {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {

            throw new IllegalArgumentException(
                    "Email already registered"
            );
        }

        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        String hashedPassword =
                passwordEncoder.encode(
                        user.getPassword()
                );

        user.setPassword(hashedPassword);

        return userRepository.save(user);
    }

    // =========================
    // LOGIN
    // =========================

    public String loginUser(
            String email,
            String password,
            String ipAddress,
            String device) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        // Check email and password
        if (user != null &&
                passwordEncoder.matches(
                        password,
                        user.getPassword()
                )) {

            // Analyze login risk
            String riskLevel =
                    securityEventService.analyzeLogin(
                            email,
                            ipAddress,
                            device
                    );

            // Record successful login
            securityEventService.recordLogin(
                    email,
                    "SUCCESS",
                    ipAddress,
                    device
            );

            // Record anomaly and create notification
            if ("MEDIUM".equals(riskLevel) ||
                    "HIGH".equals(riskLevel)) {

                // Save anomaly event
                securityEventService.recordLoginAnomaly(
                        email,
                        riskLevel,
                        ipAddress,
                        device
                );

                // Create security notification
                String title;
                String message;

                if ("HIGH".equals(riskLevel)) {

                    title = "High-Risk Login Detected";

                    message =
                            "A high-risk login was detected for your "
                            + "SecureVault account. Please review your "
                            + "login activity and active sessions.";

                } else {

                    title = "Unusual Login Detected";

                    message =
                            "An unusual login was detected for your "
                            + "SecureVault account. Please review your "
                            + "login activity.";
                }

                notificationService.createNotification(
                        email,
                        "LOGIN_ANOMALY",
                        title,
                        message,
                        riskLevel
                );
            }

            // Check MFA
            Optional<Mfa> optionalMfa =
                    mfaRepository.findByUser(user);

            if (optionalMfa.isPresent() &&
                    optionalMfa.get().isEnabled()) {

                return jwtService.generateMfaToken(
                        user.getEmail()
                );
            }

            // Create session for normal login
            String sessionId =
                    sessionService.createSession(
                            user.getEmail(),
                            device,
                            ipAddress
                    );

            // Generate session-bound JWT
            return jwtService.generateToken(
                    user.getEmail(),
                    user.getRole().name(),
                    sessionId
            );
        }

        // Failed login
        securityEventService.recordLogin(
                email,
                "FAILED",
                ipAddress,
                device
        );

        return null;
    }

    // =========================
    // COMPLETE MFA LOGIN
    // =========================

    public String completeMfaLogin(
            String email,
            String ipAddress,
            String device) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        // Create session after successful MFA
        String sessionId =
                sessionService.createSession(
                        user.getEmail(),
                        device,
                        ipAddress
                );

        // Generate final session-bound JWT
        return jwtService.generateToken(
                user.getEmail(),
                user.getRole().name(),
                sessionId
        );
    }

    // =========================
    // CREATE PASSWORD RESET TOKEN
    // =========================

    public String createPasswordResetToken(String email) {

        if (email == null ||
                email.trim().isEmpty()) {

            return null;
        }

        User user =
                userRepository
                        .findByEmail(email.trim())
                        .orElse(null);

        if (user == null) {
            return null;
        }

        byte[] tokenBytes =
                new byte[32];

        secureRandom.nextBytes(tokenBytes);

        String token =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(tokenBytes);

        user.setPasswordResetTokenHash(
                hashToken(token)
        );

        user.setPasswordResetTokenExpiry(
                Instant.now()
                        .plus(15, ChronoUnit.MINUTES)
        );

        userRepository.save(user);

        return token;
    }

    // =========================
    // RESET PASSWORD
    // =========================

    public void resetPassword(
            String token,
            String newPassword) {

        if (token == null ||
                token.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Reset token is required"
            );
        }

        if (newPassword == null ||
                newPassword.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        String tokenHash =
                hashToken(token);

        User user =
                userRepository
                        .findByPasswordResetTokenHash(tokenHash)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Invalid or expired reset token"
                                )
                        );

        Instant expiry =
                user.getPasswordResetTokenExpiry();

        if (expiry == null ||
                expiry.isBefore(Instant.now())) {

            user.setPasswordResetTokenHash(null);
            user.setPasswordResetTokenExpiry(null);

            userRepository.save(user);

            throw new IllegalArgumentException(
                    "Reset token has expired"
            );
        }

        if (passwordHistoryService.wasPasswordUsedBefore(
                user,
                newPassword)) {

            throw new IllegalArgumentException(
                    "You cannot reuse a previous password"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(
                        newPassword
                );

        passwordHistoryService.savePasswordHistory(user, user.getPassword());
        user.setPassword(hashedPassword);

        // One-time token
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

    // =========================
    // GET USER
    // =========================

    public User getUserByEmail(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow();
    }

    // =========================
    // UPDATE ROLE
    // =========================

    public User updateUserRole(
            String email,
            Role role) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        user.setRole(role);

        return userRepository.save(user);
    }

    // =========================
    // HASH TOKEN
    // =========================

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64.getEncoder()
                    .encodeToString(hash);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Could not hash reset token",
                    e
            );
        }
    }
}
