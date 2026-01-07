package com.hdbank.auth_service.controller;

import com.hdbank.auth_service.dto.UserInfo;
import com.hdbank.auth_service.dto.request.ChangePasswordRequest;
import com.hdbank.auth_service.dto.request.LoginRequest;
import com.hdbank.auth_service.dto.request.RefreshTokenRequest;
import com.hdbank.auth_service.dto.request.RegisterRequest;
import com.hdbank.auth_service.dto.response.ApiDataResponse;
import com.hdbank.auth_service.dto.response.LoginResponse;
import com.hdbank.auth_service.service.AuthService;
import com.hdbank.auth_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "User register", description = "Create user and return jwt tokens")
    public ResponseEntity<ApiDataResponse<UserInfo>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest
    ) {
        UserInfo response = authService.register(request);
        return ResponseEntity.ok(ApiDataResponse.success("Register successful", response));
    }


    @PostMapping("/login")
    @Operation(summary = "User login", description = " Authenticate user and return jwt tokens")
    public ResponseEntity<ApiDataResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
            ) {
        String ipAddress = getClientIP(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiDataResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiDataResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = authService
                .refreshToken(request.getRefreshToken(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiDataResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "User logout", description = "Revoke refresh token")
    public ResponseEntity<ApiDataResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiDataResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user", description = "Get authenticated user information")
    public ResponseEntity<ApiDataResponse<UserInfo>> getCurrentUser(Authentication authentication) {
        UserInfo userInfo = userService.getUserInfo(authentication.getName());
        return ResponseEntity.ok(ApiDataResponse.success("User retrieved successfully", userInfo));
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password", description = "Change user password")
    public ResponseEntity<ApiDataResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        userService.changePassword(
                authentication.getName(),
                request
        );
        return ResponseEntity.ok(ApiDataResponse.success("Password changed successfully", null));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
