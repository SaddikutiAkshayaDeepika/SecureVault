package com.securevault.controller;

import com.securevault.entity.Mfa;
import com.securevault.entity.User;
import com.securevault.service.EmailService;
import com.securevault.service.MfaService;
import com.securevault.service.SecurityEventService;
import com.securevault.service.UserService;
import com.securevault.security.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@RestController
@CrossOrigin(
        origins = "${FRONTEND_URL:http://localhost:5177}"
)
public class SecureVaultController {

    private final UserService userService;
    private final MfaService mfaService;
    private final EmailService emailService;
    private final SecurityEventService securityEventService;
    private final JwtService jwtService;

    public SecureVaultController(
            UserService userService,
            MfaService mfaService,
            SecurityEventService securityEventService,
            JwtService jwtService,
            EmailService emailService) {

        this.userService = userService;
        this.mfaService = mfaService;
        this.securityEventService = securityEventService;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @GetMapping("/")
    public String home() {
        return "SecureVault Backend is running successfully!";
    }

    @GetMapping("/api/test")
    public String test() {
        return "Frontend connected to Backend successfully!";
    }

    // =========================
    // AUTHENTICATION
    // =========================

    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(
            @RequestBody User user) {

        try {
            userService.registerUser(user);

            return ResponseEntity.ok(
                    "User registered successfully"
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<String> login(
            @RequestBody User user,
            jakarta.servlet.http.HttpServletRequest request) {

        String ipAddress =
                request.getRemoteAddr();

        String device =
                request.getHeader("User-Agent");

        String token =
                userService.loginUser(
                        user.getEmail(),
                        user.getPassword(),
                        ipAddress,
                        device
                );

        if (token != null) {
            return ResponseEntity.ok(token);
        }

        return ResponseEntity
                .status(401)
                .body("Invalid email or password");
    }

    @PostMapping("/api/auth/mfa-login")
    public ResponseEntity<?> completeMfaLogin(
            @RequestParam int code,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            jakarta.servlet.http.HttpServletRequest request) {

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            return ResponseEntity
                    .status(401)
                    .body("MFA token is required");
        }

        String mfaToken =
                authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(mfaToken)) {

            return ResponseEntity
                    .status(401)
                    .body("Invalid or expired MFA token");
        }

        if (jwtService.isMfaVerified(mfaToken)) {

            return ResponseEntity
                    .badRequest()
                    .body("MFA is already verified");
        }

        String email =
                jwtService.extractEmail(mfaToken);

        User user =
                userService.getUserByEmail(email);

        boolean valid =
                mfaService.verifyCode(
                        user,
                        code
                );

        if (!valid) {

            return ResponseEntity
                    .status(401)
                    .body("Invalid MFA code");
        }

        String ipAddress =
                request.getRemoteAddr();

        String device =
                request.getHeader("User-Agent");

        String finalToken =
                userService.completeMfaLogin(
                        email,
                        ipAddress,
                        device
                );

        return ResponseEntity.ok(finalToken);
    }

    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody java.util.Map<String, String> request) {

        String email =
                request.get("email");

        String resetToken =
                userService.createPasswordResetToken(email);

        if (resetToken != null &&
                email != null &&
                !email.trim().isEmpty()) {

            emailService.sendPasswordResetEmail(
                    email.trim(),
                    resetToken
            );
        }

        java.util.Map<String, Object> response =
                new java.util.HashMap<>();

        response.put(
                "message",
                "If an account exists, a password reset request has been created."
        );

        boolean returnToken =
                Boolean.parseBoolean(
                        System.getenv().getOrDefault(
                                "SECUREVAULT_RESET_RETURN_TOKEN",
                                "false"
                        )
                );

        // Local development/testing only
        if (returnToken &&
                resetToken != null) {

            response.put(
                    "resetToken",
                    resetToken
            );
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody java.util.Map<String, String> request) {

        try {

            String token =
                    request.get("token");

            String newPassword =
                    request.get("newPassword");

            userService.resetPassword(
                    token,
                    newPassword
            );

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "message",
                            "Password reset successfully"
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            java.util.Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    @GetMapping("/api/auth/me")
    public String getCurrentUser() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    // =========================
    // MFA
    // =========================

    @PostMapping("/api/mfa/enable")
    public ResponseEntity<?> enableMfa() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userService.getUserByEmail(email);

        Mfa mfa =
                mfaService.enableMfa(user);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "enabled",
                        mfa.isEnabled(),
                        "secret",
                        mfa.getSecret()
                )
        );
    }

    @GetMapping("/api/mfa/qr")
    public ResponseEntity<?> getMfaQrCode() {

        try {

            String email =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .getName();

            User user =
                    userService.getUserByEmail(email);

            String qrCodeUrl =
                    mfaService.getQrCodeUrl(user);

            if (qrCodeUrl == null) {

                return ResponseEntity
                        .badRequest()
                        .body("MFA is not enabled");
            }

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "qrCodeUrl",
                            qrCodeUrl
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(500)
                    .body(
                            "QR generation failed: "
                                    + e.getClass().getSimpleName()
                                    + " - "
                                    + e.getMessage()
                    );
        }
    }

    @GetMapping("/api/mfa/status")
    public ResponseEntity<?> getMfaStatus() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userService.getUserByEmail(email);

        Optional<Mfa> mfa =
                mfaService.getMfa(user);

        if (mfa.isPresent()) {

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "enabled",
                            mfa.get().isEnabled()
                    )
            );
        }

        return ResponseEntity.ok(
                java.util.Map.of(
                        "enabled",
                        false
                )
        );
    }

    @PostMapping("/api/mfa/verify")
    public ResponseEntity<?> verifyMfa(
            @RequestParam int code) {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userService.getUserByEmail(email);

        boolean valid =
                mfaService.verifyCode(
                        user,
                        code
                );

        if (!valid) {

            return ResponseEntity
                    .status(401)
                    .body("Invalid MFA code");
        }

        return ResponseEntity.ok(
                "MFA verification successful"
        );
    }

    @PostMapping("/api/mfa/disable")
    public ResponseEntity<?> disableMfa() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User user =
                userService.getUserByEmail(email);

        mfaService.disableMfa(user);

        return ResponseEntity.ok(
                "MFA disabled successfully"
        );
    }

    // =========================
    // LOGIN HISTORY
    // =========================

    @GetMapping("/api/security/login-history")
    public ResponseEntity<?> getLoginHistory() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return ResponseEntity.ok(
                securityEventService
                        .getLoginHistory(email)
        );
    }
}