package com.hdbank.auth_service.service;

import com.hdbank.auth_service.dto.UserInfo;
import com.hdbank.auth_service.dto.request.LoginRequest;
import com.hdbank.auth_service.dto.request.RegisterRequest;
import com.hdbank.auth_service.dto.response.LoginResponse;
import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.entity.AppUser;
import com.hdbank.auth_service.entity.RefreshToken;
import com.hdbank.auth_service.exception.RoleNotFoundException;
import com.hdbank.auth_service.exception.UserAlreadyExistsException;
import com.hdbank.auth_service.repository.AppRoleRepository;
import com.hdbank.auth_service.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final AppRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Transactional
    public UserInfo register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        AppRole userRole = roleRepository.findByNameAndDeletedFalse("USER")
                .orElseThrow(() -> new RoleNotFoundException("Default role USER not found"));

        AppUser newUser = AppUser.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        AppUser savedUser = userRepository.save(newUser);
        log.info("New user registered: {}", savedUser.getUsername());

        return UserInfo.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .enabled(savedUser.isEnabled())
                .roles(savedUser.getRoles().stream()
                        .map(AppRole::getName)
                        .collect(Collectors.toSet()))
                .build();
    }
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
                )
        );

        AppUser user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);

        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(AppRole::getName)
                        .collect(Collectors.toSet()))
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userInfo(userInfo)
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshTokenStr, String ipAddress, String userAgent) {
        RefreshToken refreshToken = refreshTokenService
                .verifyRefreshToken(refreshTokenStr);
        AppUser user = refreshToken.getUser();

        refreshTokenService.revokeRefreshToken(refreshTokenStr);

        String newAccessToken = jwtService
                .generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService
                .createRefreshToken(user, ipAddress, userAgent);

        UserInfo userInfo = UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(AppRole::getName)
                        .collect(Collectors.toSet()))
                .build();
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userInfo(userInfo)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }
}
