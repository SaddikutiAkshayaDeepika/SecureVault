package com.securevault.controller;

import com.securevault.entity.Role;
import com.securevault.entity.User;
import com.securevault.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final UserService userService;

    public RoleController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyRole(
            java.security.Principal principal) {

        User user =
                userService.getUserByEmail(
                        principal.getName()
                );

        return ResponseEntity.ok(
                Map.of(
                        "email",
                        user.getEmail(),
                        "role",
                        user.getRole().name()
                )
        );
    }

    @PutMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRole(
            @PathVariable String email,
            @RequestBody Map<String, String> request) {

        try {

            String roleValue =
                    request.get("role");

            if (roleValue == null ||
                    roleValue.trim().isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "Role is required"
                                )
                        );
            }

            Role role =
                    Role.valueOf(
                            roleValue
                                    .trim()
                                    .toUpperCase()
                    );

            User user =
                    userService.updateUserRole(
                            email,
                            role
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Role updated successfully",
                            "email",
                            user.getEmail(),
                            "role",
                            user.getRole().name()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }
}