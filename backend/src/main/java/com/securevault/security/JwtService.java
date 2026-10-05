package com.securevault.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(
            @Value("${securevault.jwt.secret}")
            String secret) {

        this.secretKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes()
                );
    }

    // Generate verified JWT with role
    public String generateToken(
            String email,
            String role) {

        return Jwts.builder()
                .setSubject(email)
                .claim("mfaVerified", true)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 60 * 24
                        )
                )
                .signWith(
                        secretKey,
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    // Generate verified JWT with role + session ID
    public String generateToken(
            String email,
            String role,
            String sessionId) {

        return Jwts.builder()
                .setSubject(email)
                .claim("mfaVerified", true)
                .claim("role", role)
                .claim("sessionId", sessionId)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 60 * 24
                        )
                )
                .signWith(
                        secretKey,
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    // Temporary MFA token
    public String generateMfaToken(String email) {

        return Jwts.builder()
                .setSubject(email)
                .claim("mfaVerified", false)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000L * 60 * 5
                        )
                )
                .signWith(
                        secretKey,
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    // Extract email
    public String extractEmail(String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    // Extract role
    public String extractRole(String token) {

        return extractClaim(
                token,
                claims -> claims.get(
                        "role",
                        String.class
                )
        );
    }

    // Extract session ID
    public String extractSessionId(String token) {

        return extractClaim(
                token,
                claims -> claims.get(
                        "sessionId",
                        String.class
                )
        );
    }

    // Check MFA status
    public boolean isMfaVerified(String token) {

        Boolean verified =
                extractClaim(
                        token,
                        claims -> claims.get(
                                "mfaVerified",
                                Boolean.class
                        )
                );

        return Boolean.TRUE.equals(verified);
    }

    // Validate token
    public boolean isTokenValid(String token) {

        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Extract claim
    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims =
                extractAllClaims(token);

        return resolver.apply(claims);
    }

    // Extract all claims
    private Claims extractAllClaims(
            String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}