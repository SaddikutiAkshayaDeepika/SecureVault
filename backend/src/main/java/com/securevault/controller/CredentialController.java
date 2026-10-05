package com.securevault.controller;

import com.securevault.entity.Credential;
import com.securevault.entity.User;
import com.securevault.repository.UserRepository;
import com.securevault.service.CredentialService;
import com.securevault.service.CredentialShareService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vault")
@CrossOrigin(
        origins = "${FRONTEND_URL:http://localhost:5177}"
)
public class CredentialController {

    private final CredentialService credentialService;
    private final CredentialShareService credentialShareService;
    private final UserRepository userRepository;

    public CredentialController(
            CredentialService credentialService,
            CredentialShareService credentialShareService,
            UserRepository userRepository) {

        this.credentialService = credentialService;
        this.credentialShareService = credentialShareService;
        this.userRepository = userRepository;
    }

    // ADD CREDENTIAL
    @PostMapping("/credentials")
    public ResponseEntity<?> addCredential(
            @RequestBody Credential credential) {

        try {

            User user = getCurrentUser();

            Credential savedCredential =
                    credentialService.addCredential(
                            credential,
                            user);

            return ResponseEntity.ok(savedCredential);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // GET OWN CREDENTIALS
    @GetMapping("/credentials")
    public ResponseEntity<?> getCredentials() {

        try {

            User user = getCurrentUser();

            return ResponseEntity.ok(
                    credentialService.getUserCredentials(user)
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // SEARCH OWN CREDENTIALS
    @GetMapping("/credentials/search")
    public ResponseEntity<?> searchCredentials(
            @RequestParam String query) {
        try {
            User user = getCurrentUser();

            return ResponseEntity.ok(
                    credentialService.searchCredentials(
                            user,
                            query));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // GET SINGLE CREDENTIAL
    //
    // IMPORTANT:
    // 1. Load encrypted credential
    // 2. Check owner / permission
    // 3. Decrypt only if access is allowed
    @GetMapping("/credentials/{id}")
    public ResponseEntity<?> getCredential(
            @PathVariable Long id) {

        try {

            User user = getCurrentUser();

            // Load credential WITHOUT decrypting
            Credential credential =
                    credentialService.getCredentialById(id);

            // OWNER
            if (credential.getUser()
                    .getId()
                    .equals(user.getId())) {

                Credential decryptedCredential =
                        credentialService.decryptCredential(
                                credential);

                return ResponseEntity.ok(
                        decryptedCredential);
            }

            // SHARED USER
            boolean allowed =
                    credentialShareService.hasPermission(
                            id,
                            user,
                            "VIEW_ONLY");

            if (!allowed) {

                return ResponseEntity.status(403)
                        .body(
                                "You do not have permission to view this credential"
                        );
            }

            // Decrypt ONLY after permission check
            Credential decryptedCredential =
                    credentialService.decryptCredential(
                            credential);

            return ResponseEntity.ok(
                    decryptedCredential);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(404)
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // UPDATE CREDENTIAL
    @PutMapping("/credentials/{id}")
    public ResponseEntity<?> updateCredential(
            @PathVariable Long id,
            @RequestBody Credential credential) {

        try {

            User user = getCurrentUser();

            try {

                // OWNER UPDATE
                Credential updatedCredential =
                        credentialService.updateCredential(
                                id,
                                credential,
                                user);

                return ResponseEntity.ok(
                        updatedCredential);

            } catch (IllegalArgumentException e) {

                // SHARED USER WITH EDIT PERMISSION
                boolean allowed =
                        credentialShareService.hasPermission(
                                id,
                                user,
                                "EDIT");

                if (!allowed) {

                    return ResponseEntity.status(403)
                            .body(
                                    "You do not have permission to edit this credential"
                            );
                }

                Credential updatedCredential =
                        credentialService.updateSharedCredential(
                                id,
                                credential);

                return ResponseEntity.ok(
                        updatedCredential);
            }

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // DELETE CREDENTIAL
    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<?> deleteCredential(
            @PathVariable Long id) {

        try {

            User user = getCurrentUser();

            try {

                // OWNER DELETE
                credentialService.deleteCredential(
                        id,
                        user);

                return ResponseEntity.ok(
                        "Credential deleted successfully");

            } catch (IllegalArgumentException e) {

                // SHARED USER WITH FULL MANAGEMENT
                boolean allowed =
                        credentialShareService.hasPermission(
                                id,
                                user,
                                "FULL_MANAGEMENT");

                if (!allowed) {

                    return ResponseEntity.status(403)
                            .body(
                                    "You do not have permission to delete this credential"
                            );
                }

                credentialService.deleteSharedCredential(
                        id);

                return ResponseEntity.ok(
                        "Credential deleted successfully");
            }

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // GET CURRENT USER
    private User getCurrentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));
    }
}