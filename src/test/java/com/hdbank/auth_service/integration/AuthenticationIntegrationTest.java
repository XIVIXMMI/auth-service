package com.hdbank.auth_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.auth_service.dto.UserInfo;
import com.hdbank.auth_service.dto.request.LoginRequest;
import com.hdbank.auth_service.dto.request.RefreshTokenRequest;
import com.hdbank.auth_service.dto.request.RegisterRequest;
import com.hdbank.auth_service.dto.response.ApiDataResponse;
import com.hdbank.auth_service.dto.response.LoginResponse;
import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.repository.AppRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppRoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByNameAndDeletedFalse("USER").isEmpty()) {
            AppRole userRole = new AppRole();
            userRole.setName("USER");
            userRole.setDescription("Default user role");
            userRole.setDeleted(false);
            roleRepository.save(userRole);
        }
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void testUserRegistration() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser",
                "password123",
                "Test User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Register successful"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.full_name").value("Test User"))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @DisplayName("Should fail registration with invalid username (too short)")
    void testRegistrationWithInvalidUsername() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "ab",
                "password123",
                "Test User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail registration with invalid password (too short)")
    void testRegistrationWithInvalidPassword() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "testuser2",
                "pass",
                "Test User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testUserLogin() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "loginuser",
                "password123",
                "Login User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest("loginuser", "password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(jsonPath("$.data.refresh_token").exists())
                .andExpect(jsonPath("$.data.user_info.username").value("loginuser"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        ApiDataResponse<LoginResponse> response = objectMapper.readValue(
                responseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiDataResponse.class,
                        LoginResponse.class
                )
        );

        assertThat(response.getData().getAccessToken()).isNotEmpty();
        assertThat(response.getData().getRefreshToken()).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void testLoginWithInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should successfully refresh access token")
    void testRefreshToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "refreshuser",
                "password123",
                "Refresh User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest("refreshuser", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        ApiDataResponse<LoginResponse> loginResponse = objectMapper.readValue(
                loginResponseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiDataResponse.class,
                        LoginResponse.class
                )
        );

        String refreshToken = loginResponse.getData().getRefreshToken();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.access_token").exists())
                .andExpect(jsonPath("$.data.refresh_token").exists());
    }

    @Test
    @DisplayName("Should fail refresh with invalid token")
    void testRefreshWithInvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("invalid-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should successfully logout")
    void testLogout() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "logoutuser",
                "password123",
                "Logout User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest("logoutuser", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponseBody = loginResult.getResponse().getContentAsString();
        ApiDataResponse<LoginResponse> loginResponse = objectMapper.readValue(
                loginResponseBody,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiDataResponse.class,
                        LoginResponse.class
                )
        );

        String accessToken = loginResponse.getData().getAccessToken();
        String refreshToken = loginResponse.getData().getRefreshToken();

        RefreshTokenRequest logoutRequest = new RefreshTokenRequest(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }
}
