
package com.securevault.service;

import com.securevault.entity.Mfa;
import com.securevault.entity.User;
import com.securevault.repository.MfaRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class MfaService {

    private final MfaRepository mfaRepository;
    private final GoogleAuthenticator googleAuthenticator;

    public MfaService(MfaRepository mfaRepository) {
        this.mfaRepository = mfaRepository;
        this.googleAuthenticator = new GoogleAuthenticator();
    }

    public Mfa enableMfa(User user) {

        Optional<Mfa> existingMfa =
                mfaRepository.findByUser(user);

        Mfa mfa;

        if (existingMfa.isPresent()) {
            mfa = existingMfa.get();
        } else {
            mfa = new Mfa();
            mfa.setUser(user);
        }

        GoogleAuthenticatorKey key =
                googleAuthenticator.createCredentials();

        mfa.setSecret(key.getKey());
        mfa.setEnabled(true);

        return mfaRepository.save(mfa);
    }

    public Optional<Mfa> getMfa(User user) {
        return mfaRepository.findByUser(user);
    }

    public String getQrCodeUrl(User user) {

        Optional<Mfa> optionalMfa =
                mfaRepository.findByUser(user);

        if (optionalMfa.isEmpty()) {
            return null;
        }

        Mfa mfa = optionalMfa.get();

        if (!mfa.isEnabled() ||
                mfa.getSecret() == null ||
                mfa.getSecret().isBlank()) {

            return null;
        }

        String email =
                URLEncoder.encode(
                        user.getEmail(),
                        StandardCharsets.UTF_8
                );

        String issuer =
                URLEncoder.encode(
                        "SecureVault",
                        StandardCharsets.UTF_8
                );

        return "otpauth://totp/"
                + issuer
                + ":"
                + email
                + "?secret="
                + mfa.getSecret()
                + "&issuer="
                + issuer;
    }

    public boolean verifyCode(User user, int code) {

        Optional<Mfa> optionalMfa =
                mfaRepository.findByUser(user);

        if (optionalMfa.isEmpty()) {
            return false;
        }

        Mfa mfa = optionalMfa.get();

        if (!mfa.isEnabled() ||
                mfa.getSecret() == null ||
                mfa.getSecret().isBlank()) {

            return false;
        }

        return googleAuthenticator.authorize(
                mfa.getSecret(),
                code
        );
    }

    public void disableMfa(User user) {

        Optional<Mfa> optionalMfa =
                mfaRepository.findByUser(user);

        if (optionalMfa.isPresent()) {

            Mfa mfa = optionalMfa.get();

            mfa.setEnabled(false);

            mfaRepository.save(mfa);
        }
    }
}

