package com.hdbank.auth_service.service;

import com.hdbank.auth_service.dto.UserInfo;
import com.hdbank.auth_service.dto.request.ChangePasswordRequest;
import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.entity.AppUser;
import com.hdbank.auth_service.exception.InvalidPasswordException;
import com.hdbank.auth_service.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenService refreshTokenService;

    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AppUser findByUserName(String username){
        return appUserRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public UserInfo getUserInfo(String username){
        AppUser user = findByUserName(username);
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(user.getRoles()
                        .stream()
                        .map(AppRole::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request){
        AppUser user = findByUserName(username);
        if(!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())){
            throw new InvalidPasswordException("Old password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(user);
        refreshTokenService.revokeAllByUserId(user.getId());

        log.info("Password changed and all tokens revoked for user: {}", username);
    }
}
