package com.securevault.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    // ---------------------------------------------------------
    // NEW AES-256 KEY
    // ---------------------------------------------------------

    @Value("${securevault.aes.key}")
    private String newEncodedKey;

    // ---------------------------------------------------------
    // ORIGINAL HISTORICAL KEY
    // Used only to decrypt old credentials during migration
    // ---------------------------------------------------------

    @Value("${securevault.aes.original-key}")
    private String originalKey;

    // ---------------------------------------------------------
    // LEGACY KEY
    // Used only to decrypt older credentials
    // ---------------------------------------------------------

    @Value("${securevault.aes.legacy-key}")
    private String legacyKey;

    // ---------------------------------------------------------
    // CONSTANTS
    // ---------------------------------------------------------

    private static final String AES = "AES";

    private static final String GCM_TRANSFORMATION =
            "AES/GCM/NoPadding";

    private static final String ECB_TRANSFORMATION =
            "AES/ECB/PKCS5Padding";

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 128;

    // Prefix used for newly encrypted credentials
    private static final String GCM_PREFIX = "v2:";

    // Secure random generator for GCM IVs
    private final SecureRandom secureRandom =
            new SecureRandom();

    // ---------------------------------------------------------
    // NEW AES-256 KEY
    // Base64 encoded environment variable
    // ---------------------------------------------------------

    private SecretKeySpec getNewKey() {

        try {

            byte[] keyBytes =
                    Base64.getDecoder()
                            .decode(newEncodedKey);

            if (keyBytes.length != 32) {

                throw new IllegalArgumentException(
                        "AES-256 key must be exactly 32 bytes"
                );
            }

            return new SecretKeySpec(
                    keyBytes,
                    AES
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid Base64 AES-256 key",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // ORIGINAL HISTORICAL KEY
    // Plain UTF-8 32-byte key from old implementation
    // ---------------------------------------------------------

    private SecretKeySpec getOriginalKey() {

        byte[] keyBytes =
                originalKey.getBytes(
                        StandardCharsets.UTF_8
                );

        if (keyBytes.length != 16 &&
            keyBytes.length != 24 &&
            keyBytes.length != 32) {

            throw new IllegalArgumentException(
                    "Original AES key must be 16, 24, or 32 bytes"
            );
        }

        return new SecretKeySpec(
                keyBytes,
                AES
        );
    }

    // ---------------------------------------------------------
    // LEGACY KEY
    // Stored as Base64 in environment variable
    // ---------------------------------------------------------

    private SecretKeySpec getLegacyKey() {

        try {

            byte[] keyBytes =
                    Base64.getDecoder()
                            .decode(legacyKey);

            if (keyBytes.length != 16 &&
                keyBytes.length != 24 &&
                keyBytes.length != 32) {

                throw new IllegalArgumentException(
                        "Legacy AES key must decode to 16, 24, or 32 bytes"
                );
            }

            return new SecretKeySpec(
                    keyBytes,
                    AES
            );

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid Base64 legacy AES key",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // NEW ENCRYPTION
    // AES-256-GCM
    // ---------------------------------------------------------

    public String encrypt(String password) {

        if (password == null) {

            throw new IllegalArgumentException(
                    "Password cannot be null"
            );
        }

        try {

            SecretKeySpec key =
                    getNewKey();

            // Generate a unique random IV
            byte[] iv =
                    new byte[GCM_IV_LENGTH];

            secureRandom.nextBytes(iv);

            GCMParameterSpec gcmSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            GCM_TRANSFORMATION
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    gcmSpec
            );

            byte[] encrypted =
                    cipher.doFinal(
                            password.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            // Store:
            // IV + encrypted data + authentication tag
            byte[] combined =
                    new byte[
                            iv.length +
                            encrypted.length
                    ];

            System.arraycopy(
                    iv,
                    0,
                    combined,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encrypted,
                    0,
                    combined,
                    iv.length,
                    encrypted.length
            );

            return GCM_PREFIX +
                    Base64.getEncoder()
                            .encodeToString(combined);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Encryption failed",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // DECRYPTION
    // ---------------------------------------------------------

    public String decrypt(String encryptedPassword) {

        if (encryptedPassword == null) {

            throw new IllegalArgumentException(
                    "Encrypted password cannot be null"
            );
        }

        // -----------------------------------------------------
        // 1. NEW GCM FORMAT
        // -----------------------------------------------------

        if (encryptedPassword.startsWith(GCM_PREFIX)) {

            return decryptGcm(
                    encryptedPassword.substring(
                            GCM_PREFIX.length()
                    )
            );
        }

        // -----------------------------------------------------
        // 2. ORIGINAL HISTORICAL KEY
        // -----------------------------------------------------

        try {

            return decryptOldEcbWithOriginalKey(
                    encryptedPassword
            );

        } catch (Exception originalKeyException) {

            // -------------------------------------------------
            // 3. OLD NEW-KEY FORMAT
            // -------------------------------------------------

            try {

                return decryptOldEcbWithNewKey(
                        encryptedPassword
                );

            } catch (Exception newKeyException) {

                // ---------------------------------------------
                // 4. LEGACY KEY FORMAT
                // ---------------------------------------------

                try {

                    return decryptOldEcbWithLegacyKey(
                            encryptedPassword
                    );

                } catch (Exception legacyException) {

                    throw new RuntimeException(
                            "Decryption failed with all supported keys.",
                            legacyException
                    );
                }
            }
        }
    }

    // ---------------------------------------------------------
    // GCM DECRYPTION
    // ---------------------------------------------------------

    private String decryptGcm(
            String encodedData) {

        try {

            byte[] combined =
                    Base64.getDecoder()
                            .decode(encodedData);

            if (combined.length <= GCM_IV_LENGTH) {

                throw new IllegalArgumentException(
                        "Invalid GCM encrypted data"
                );
            }

            byte[] iv =
                    Arrays.copyOfRange(
                            combined,
                            0,
                            GCM_IV_LENGTH
                    );

            byte[] encrypted =
                    Arrays.copyOfRange(
                            combined,
                            GCM_IV_LENGTH,
                            combined.length
                    );

            SecretKeySpec key =
                    getNewKey();

            GCMParameterSpec gcmSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH,
                            iv
                    );

            Cipher cipher =
                    Cipher.getInstance(
                            GCM_TRANSFORMATION
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    gcmSpec
            );

            byte[] decrypted =
                    cipher.doFinal(
                            encrypted
                    );

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "GCM decryption failed",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // ORIGINAL HISTORICAL KEY DECRYPTION
    // ---------------------------------------------------------

    private String decryptOldEcbWithOriginalKey(
            String encryptedPassword) {

        try {

            SecretKeySpec key =
                    getOriginalKey();

            Cipher cipher =
                    Cipher.getInstance(
                            ECB_TRANSFORMATION
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key
            );

            byte[] decrypted =
                    cipher.doFinal(
                            Base64.getDecoder()
                                    .decode(
                                            encryptedPassword
                                    )
                    );

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Original AES key decryption failed",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // OLD NEW-KEY ECB DECRYPTION
    // ---------------------------------------------------------

    private String decryptOldEcbWithNewKey(
            String encryptedPassword) {

        try {

            SecretKeySpec key =
                    getNewKey();

            Cipher cipher =
                    Cipher.getInstance(
                            ECB_TRANSFORMATION
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key
            );

            byte[] decrypted =
                    cipher.doFinal(
                            Base64.getDecoder()
                                    .decode(
                                            encryptedPassword
                                    )
                    );

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Old new-key decryption failed",
                    e
            );
        }
    }

    // ---------------------------------------------------------
    // LEGACY ECB DECRYPTION
    // ---------------------------------------------------------

    private String decryptOldEcbWithLegacyKey(
            String encryptedPassword) {

        try {

            SecretKeySpec key =
                    getLegacyKey();

            Cipher cipher =
                    Cipher.getInstance(
                            ECB_TRANSFORMATION
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key
            );

            byte[] decrypted =
                    cipher.doFinal(
                            Base64.getDecoder()
                                    .decode(
                                            encryptedPassword
                                    )
                    );

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Legacy AES decryption failed",
                    e
            );
        }
    }
}