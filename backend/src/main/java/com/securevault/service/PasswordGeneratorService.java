package com.securevault.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PasswordGeneratorService {

    private final SecureRandom random = new SecureRandom();

    public String generatePassword(
            int length,
            boolean uppercase,
            boolean lowercase,
            boolean numbers,
            boolean special) {

        String characters = "";

        if (uppercase) {
            characters += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        }

        if (lowercase) {
            characters += "abcdefghijklmnopqrstuvwxyz";
        }

        if (numbers) {
            characters += "0123456789";
        }

        if (special) {
            characters += "!@#$%^&*";
        }

        if (characters.isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one character type"
            );
        }

        if (length < 4 || length > 64) {
            throw new IllegalArgumentException(
                    "Password length must be between 4 and 64"
            );
        }

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {

            int index = random.nextInt(characters.length());

            password.append(characters.charAt(index));
        }

        return password.toString();
    }
}