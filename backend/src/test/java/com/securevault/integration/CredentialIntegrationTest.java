package com.securevault.integration;

import com.securevault.config.TestMailConfig;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.securevault.SecureVaultApplication.class, TestMailConfig.class}, properties = "spring.mail.username=test@securevault.local")
class CredentialIntegrationTest {

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


    @Test
    void searchCredentials_byTitle_shouldReturnMatchingCredential() throws Exception {
        String email =
                "searchtitle" + System.currentTimeMillis() + "@test.com";
        String token = registerAndLogin(email);

        String body = """
                {
                    "title": "GitHub Search Test",
                    "username": "githubuser",
                    "password": "GitHub@12345"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk());

        MvcResult result = mockMvc()
                .perform(
                        get("/api/vault/credentials/search")
                                .param("query", "GitHub")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(
                result.getResponse().getContentAsString()
                        .contains("GitHub Search Test")
        );
    }

    @Test
    void searchCredentials_byUsername_shouldReturnMatchingCredential() throws Exception {
        String email =
                "searchuser" + System.currentTimeMillis() + "@test.com";
        String token = registerAndLogin(email);

        String body = """
                {
                    "title": "Search Username Test",
                    "username": "uniqueSearchUser",
                    "password": "Test@12345"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk());

        MvcResult result = mockMvc()
                .perform(
                        get("/api/vault/credentials/search")
                                .param("query", "uniqueSearchUser")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(
                result.getResponse().getContentAsString()
                        .contains("Search Username Test")
        );
    }

    @Test
    void searchCredentials_emptyQuery_shouldReturnUserCredentials() throws Exception {
        String email =
                "searchempty" + System.currentTimeMillis() + "@test.com";
        String token = registerAndLogin(email);

        String body = """
                {
                    "title": "Empty Query Test",
                    "username": "emptyqueryuser",
                    "password": "Test@12345"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk());

        MvcResult result = mockMvc()
                .perform(
                        get("/api/vault/credentials/search")
                                .param("query", "")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(
                result.getResponse().getContentAsString()
                        .contains("Empty Query Test")
        );
    }

    @Test
    void searchCredentials_withoutAuthentication_shouldReturnUnauthorized()
            throws Exception {

        mockMvc()
                .perform(
                        get("/api/vault/credentials/search")
                                .param("query", "GitHub")
                )
                .andExpect(status().isUnauthorized());
    }


    @Test
    void createCredential_shouldReturnSuccess() throws Exception {

        String email =
                "create" + System.currentTimeMillis() + "@test.com";

        String token = registerAndLogin(email);

        String credentialBody =
                """
                {
                    "title": "GitHub",
                    "username": "testuser",
                    "password": "GitHub@12345"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(credentialBody)
                )
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();

        assertFalse(
                credentialRepository.findByUser(user).isEmpty()
        );
    }

    @Test
    void getCredentials_shouldReturnUserCredentials() throws Exception {

        String email =
                "get" + System.currentTimeMillis() + "@test.com";

        String token = registerAndLogin(email);

        String credentialBody =
                """
                {
                    "title": "Gmail",
                    "username": "testuser",
                    "password": "Gmail@12345"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(credentialBody)
                )
                .andExpect(status().isOk());

        mockMvc()
                .perform(
                        get("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());
    }

    @Test
    void updateCredential_shouldReturnSuccess() throws Exception {

        String email =
                "update" + System.currentTimeMillis() + "@test.com";

        String token = registerAndLogin(email);

        String createBody =
                """
                {
                    "title": "Old Title",
                    "username": "olduser",
                    "password": "OldPass@123"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody)
                )
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();

        Long credentialId =
                credentialRepository.findByUser(user)
                        .get(0)
                        .getId();

        String updateBody =
                """
                {
                    "title": "Updated Title",
                    "username": "updateduser",
                    "password": "UpdatedPass@123"
                }
                """;

        mockMvc()
                .perform(
                        put("/api/vault/credentials/" + credentialId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody)
                )
                .andExpect(status().isOk());
    }

    @Test
    void deleteCredential_shouldReturnSuccess() throws Exception {

        String email =
                "delete" + System.currentTimeMillis() + "@test.com";

        String token = registerAndLogin(email);

        String createBody =
                """
                {
                    "title": "Delete Test",
                    "username": "deleteuser",
                    "password": "Delete@123"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody)
                )
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email).orElseThrow();

        Long credentialId =
                credentialRepository.findByUser(user)
                        .get(0)
                        .getId();

        mockMvc()
                .perform(
                        delete("/api/vault/credentials/" + credentialId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());

        assertTrue(
                credentialRepository.findById(credentialId).isEmpty()
        );
    }

    @Test
    void createCredential_withoutAuthentication_shouldReturnUnauthorized()
            throws Exception {

        String credentialBody =
                """
                {
                    "title": "Unauthorized Test",
                    "username": "testuser",
                    "password": "Test@12345"
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(credentialBody)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCredential_invalidInput_shouldReturnBadRequest()
            throws Exception {

        String email =
                "invalid" + System.currentTimeMillis() + "@test.com";

        String token = registerAndLogin(email);

        String invalidBody =
                """
                {
                    "title": "",
                    "username": "",
                    "password": ""
                }
                """;

        mockMvc()
                .perform(
                        post("/api/vault/credentials")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidBody)
                )
                .andExpect(status().isBadRequest());
    }
}