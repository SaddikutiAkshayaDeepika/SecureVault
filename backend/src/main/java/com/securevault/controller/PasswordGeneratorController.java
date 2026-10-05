package com.securevault.controller;

import com.securevault.service.PasswordGeneratorService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password")
@CrossOrigin(
        origins = "${FRONTEND_URL:http://localhost:5177}"
)
public class PasswordGeneratorController {

    private final PasswordGeneratorService passwordGeneratorService;

    public PasswordGeneratorController(
            PasswordGeneratorService passwordGeneratorService) {

        this.passwordGeneratorService = passwordGeneratorService;
    }

    @GetMapping("/generate")
    public ResponseEntity<?> generatePassword(
            @RequestParam int length,
            @RequestParam boolean uppercase,
            @RequestParam boolean lowercase,
            @RequestParam boolean numbers,
            @RequestParam boolean special) {

        try {

            String password =
                    passwordGeneratorService.generatePassword(
                            length,
                            uppercase,
                            lowercase,
                            numbers,
                            special
                    );

            return ResponseEntity.ok(password);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}