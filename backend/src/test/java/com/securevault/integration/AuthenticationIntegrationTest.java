package com.securevault.integration;

import com.securevault.config.TestMailConfig;
import com.securevault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {com.securevault.SecureVaultApplication.class, TestMailConfig.class}, properties = "spring.mail.username=test@securevault.local")
class AuthenticationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void registerUser_shouldReturnSuccess() throws Exception {

        String email =
                "integration" + System.currentTimeMillis() + "@test.com";

        String requestBody =
                """
                {
                    "email": "%s",
                    "password": "Test@12345"
                }
                """.formatted(email);

        mockMvc()
                .perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());

        assertTrue(userRepository.findByEmail(email).isPresent());
    }

    @Test
    void registerUser_duplicateEmail_shouldReturnBadRequest() throws Exception {

        String email =
                "duplicate" + System.currentTimeMillis() + "@test.com";

        String requestBody =
                """
                {
                    "email": "%s",
                    "password": "Test@12345"
                }
                """.formatted(email);

        mockMvc()
                .perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk());

        mockMvc()
                .perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginUser_validCredentials_shouldReturnToken() throws Exception {

        String email =
                "login" + System.currentTimeMillis() + "@test.com";

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

        String loginBody =
                """
                {
                    "email": "%s",
                    "password": "Test@12345"
                }
                """.formatted(email);

        mockMvc()
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isOk());
    }

    @Test
    void loginUser_wrongPassword_shouldReturnUnauthorized() throws Exception {

        String email =
                "wrongpass" + System.currentTimeMillis() + "@test.com";

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

        String loginBody =
                """
                {
                    "email": "%s",
                    "password": "WrongPassword123"
                }
                """.formatted(email);

        mockMvc()
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUser_unknownEmail_shouldReturnUnauthorized() throws Exception {

        String email =
                "doesnotexist" + System.currentTimeMillis() + "@test.com";

        String loginBody =
                """
                {
                    "email": "%s",
                    "password": "Test@12345"
                }
                """.formatted(email);

        mockMvc()
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody)
                )
                .andExpect(status().isUnauthorized());
    }
}