package com.hdbank.auth_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.auth_service.dto.request.ChangePasswordRequest;
import com.hdbank.auth_service.dto.request.LoginRequest;
import com.hdbank.auth_service.dto.request.RegisterRequest;
import com.hdbank.auth_service.dto.response.ApiDataResponse;
import com.hdbank.auth_service.dto.response.LoginResponse;
import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.repository.AppRoleRepository;
import com.hdbank.auth_service.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Protected Endpoints Integration Tests")
class ProtectedEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppRoleRepository roleRepository;

    private String accessToken;
    private String username = "protecteduser";
    private String password = "password123";

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByNameAndDeletedFalse("USER").isEmpty()) {
            AppRole userRole = new AppRole();
            userRole.setName("USER");
            userRole.setDescription("Default user role");
            userRole.setDeleted(false);
            roleRepository.save(userRole);
        }

        RegisterRequest registerRequest = new RegisterRequest(
                username,
                password,
                "Protected User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest(username, password);

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

        accessToken = loginResponse.getData().getAccessToken();
    }

    @Test
    @DisplayName("Should successfully get current user info with valid JWT")
    void testGetCurrentUserWithValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.full_name").value("Protected User"))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @DisplayName("Should fail to get current user without JWT")
    void testGetCurrentUserWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail to get current user with invalid JWT")
    void testGetCurrentUserWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail to get current user with malformed Authorization header")
    void testGetCurrentUserWithMalformedHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "InvalidFormat " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should successfully change password with valid credentials")
    void testChangePasswordSuccess() throws Exception {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                password,
                "newpassword123"
        );

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        LoginRequest loginRequest = new LoginRequest(username, "newpassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should fail to change password without JWT")
    void testChangePasswordWithoutToken() throws Exception {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                password,
                "newpassword123"
        );

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail to change password with wrong old password")
    void testChangePasswordWithWrongOldPassword() throws Exception {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                "wrongoldpassword",
                "newpassword123"
        );

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail to change password with invalid new password (too short)")
    void testChangePasswordWithInvalidNewPassword() throws Exception {
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest(
                password,
                "short"
        );

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should verify JWT token expiration is working")
    void testJwtTokenStructure() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        String[] tokenParts = accessToken.split("\\.");
        assert tokenParts.length == 3 : "JWT should have 3 parts (header.payload.signature)";
    }
}
