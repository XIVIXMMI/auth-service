package com.hdbank.auth_service.security;

import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.entity.AppUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String username;
    private String password;
    private final String fullName;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public static CustomUserDetails fromAppUser(AppUser appUser){
        Collection<GrantedAuthority> authorities = new HashSet<>();
        if(appUser.getRoles() != null && !appUser.getRoles().isEmpty()){
            for(AppRole role : appUser.getRoles()){
                String roleName = role.getName();
                String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                authorities.add( new SimpleGrantedAuthority(authority));
            }
        }
        return new CustomUserDetails(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getPasswordHash(),
                appUser.getFullName(),
                appUser.isEnabled(),
                authorities
        );
    }

    public static CustomUserDetails fromJwtClaims(
            Long userId,
            String username,
            Collection<String> roles,
            boolean isEnabled
    ) {
        Collection<GrantedAuthority> authorities = new HashSet<>();
        for( String role : roles){
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return new CustomUserDetails(
                userId,
                username,
                null,
                null,
                isEnabled,
                authorities
        );
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
