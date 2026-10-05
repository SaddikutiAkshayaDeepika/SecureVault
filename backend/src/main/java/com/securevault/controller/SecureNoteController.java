package com.securevault.controller;

import com.securevault.entity.SecureNote;
import com.securevault.entity.User;
import com.securevault.repository.UserRepository;
import com.securevault.service.SecureNoteService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secure-notes")
public class SecureNoteController {

    private final SecureNoteService secureNoteService;
    private final UserRepository userRepository;

    public SecureNoteController(
            SecureNoteService secureNoteService,
            UserRepository userRepository) {

        this.secureNoteService = secureNoteService;
        this.userRepository = userRepository;
    }

    // CREATE NOTE
    @PostMapping
    public ResponseEntity<?> addNote(
            @RequestBody SecureNote note) {

        try {

            User user = getCurrentUser();

            return ResponseEntity.ok(
                    secureNoteService.addNote(
                            note,
                            user));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // GET OWN NOTES
    @GetMapping
    public ResponseEntity<?> getNotes() {

        try {

            User user = getCurrentUser();

            return ResponseEntity.ok(
                    secureNoteService.getUserNotes(user));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // GET SINGLE NOTE
    @GetMapping("/{id}")
    public ResponseEntity<?> getNote(
            @PathVariable Long id) {

        try {

            User user = getCurrentUser();

            return ResponseEntity.ok(
                    secureNoteService.getNoteById(
                            id,
                            user));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(403)
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // UPDATE NOTE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(
            @PathVariable Long id,
            @RequestBody SecureNote note) {

        try {

            User user = getCurrentUser();

            return ResponseEntity.ok(
                    secureNoteService.updateNote(
                            id,
                            note,
                            user));

        } catch (IllegalArgumentException e) {

            if (e.getMessage() != null &&
                    e.getMessage().contains("required")) {

                return ResponseEntity.badRequest()
                        .body(e.getMessage());
            }

            return ResponseEntity.status(403)
                    .body(e.getMessage());

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500)
                    .body("Error: " + e.getMessage());
        }
    }

    // DELETE NOTE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(
            @PathVariable Long id) {

        try {

            User user = getCurrentUser();

            secureNoteService.deleteNote(
                    id,
                    user);

            return ResponseEntity.ok(
                    "Secure note deleted successfully");

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(403)
                    .body(e.getMessage());

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
