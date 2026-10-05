package com.securevault.integration;

import com.securevault.config.TestMailConfig;
import com.securevault.entity.Credential;
import com.securevault.entity.User;
import com.securevault.repository.CredentialRepository;
import com.securevault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.securevault.SecureVaultApplication.class, TestMailConfig.class}, properties = "spring.mail.username=test@securevault.local")
class CredentialShareIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    private String registerAndLogin(String email) throws Exception {

        mockMvc()
                .perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "email": "%s",
                                    "password": "Test@12345"
                                }
                                """.formatted(email))
                )
                .andExpect(status().isOk());

        MvcResult result = mockMvc()
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "email": "%s",
                                    "password": "Test@12345"
                                }
                                """.formatted(email))
                )
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getContentAsString();
    }

    private Long createCredential(String email) {

        User user = userRepository.findByEmail(email).orElseThrow();

        Credential credential = new Credential();

        credential.setTitle("Shared GitHub");
        credential.setUsername("shareduser");
        credential.setPassword("Shared@12345");
        credential.setUser(user);

        return credentialRepository.save(credential).getId();
    }

    @Test
    void shareCredential_shouldReturnSuccess() throws Exception {

        String ownerEmail =
                "owner" + System.currentTimeMillis() + "@test.com";

        String recipientEmail =
                "recipient" + System.currentTimeMillis() + "@test.com";

        String ownerToken = registerAndLogin(ownerEmail);

        registerAndLogin(recipientEmail);

        Long credentialId = createCredential(ownerEmail);

        String expiresAt =
                LocalDateTime.now()
                        .plusDays(1)
                        .withNano(0)
                        .toString();

        mockMvc()
                .perform(
                        post("/api/vault/shares")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                                .param(
                                        "credentialId",
                                        credentialId.toString()
                                )
                                .param(
                                        "recipientEmail",
                                        recipientEmail
                                )
                                .param(
                                        "expiresAt",
                                        expiresAt
                                )
                                .param(
                                        "permission",
                                        "VIEW_ONLY"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void recipient_shouldSeeReceivedShare() throws Exception {

        String ownerEmail =
                "owner2" + System.currentTimeMillis() + "@test.com";

        String recipientEmail =
                "recipient2" + System.currentTimeMillis() + "@test.com";

        String ownerToken = registerAndLogin(ownerEmail);

        String recipientToken = registerAndLogin(recipientEmail);

        Long credentialId = createCredential(ownerEmail);

        String expiresAt =
                LocalDateTime.now()
                        .plusDays(1)
                        .withNano(0)
                        .toString();

        mockMvc()
                .perform(
                        post("/api/vault/shares")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                                .param(
                                        "credentialId",
                                        credentialId.toString()
                                )
                                .param(
                                        "recipientEmail",
                                        recipientEmail
                                )
                                .param(
                                        "expiresAt",
                                        expiresAt
                                )
                                .param(
                                        "permission",
                                        "VIEW_ONLY"
                                )
                )
                .andExpect(status().isOk());

        mockMvc()
                .perform(
                        get("/api/vault/shares")
                                .header(
                                        "Authorization",
                                        "Bearer " + recipientToken
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shareCredential_invalidPermission_shouldReturnBadRequest()
            throws Exception {

        String ownerEmail =
                "owner3" + System.currentTimeMillis() + "@test.com";

        String recipientEmail =
                "recipient3" + System.currentTimeMillis() + "@test.com";

        String ownerToken = registerAndLogin(ownerEmail);

        registerAndLogin(recipientEmail);

        Long credentialId = createCredential(ownerEmail);

        String expiresAt =
                LocalDateTime.now()
                        .plusDays(1)
                        .withNano(0)
                        .toString();

        mockMvc()
                .perform(
                        post("/api/vault/shares")
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                                .param(
                                        "credentialId",
                                        credentialId.toString()
                                )
                                .param(
                                        "recipientEmail",
                                        recipientEmail
                                )
                                .param(
                                        "expiresAt",
                                        expiresAt
                                )
                                .param(
                                        "permission",
                                        "INVALID_PERMISSION"
                                )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedUser_shouldNotAccessShares()
            throws Exception {

        mockMvc()
                .perform(
                        get("/api/vault/shares")
                )
                .andExpect(status().isUnauthorized());
    }
}