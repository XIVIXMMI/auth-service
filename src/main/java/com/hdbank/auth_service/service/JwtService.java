package com.hdbank.auth_service.service;

import com.hdbank.auth_service.entity.AppRole;
import com.hdbank.auth_service.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 *   Chức năng:
 *   - generateAccessToken(AppUser user) - Tạo JWT với claims: userId, username, roles
 *   - extractUsername(String token) - Lấy username từ token
 *   - extractUserId(String token) - Lấy userId từ token
 *   - isTokenValid(String token) - Validate token (signature + expiration)
 *   - Sử dụng HS256 algorithm với secret từ config
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateAccessToken(AppUser appUser){
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", appUser.getId());
        claims.put("username", appUser.getUsername());

        List<String> roleNames = new ArrayList<>();
        for( AppRole role : appUser.getRoles()){
            String roleName = role.getName();
            String authority = roleName.startsWith("ROLE_") ?
                    roleName : "ROLE_" + roleName;
            roleNames.add(authority);
        }
        claims.put("roles", roleNames);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(appUser.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token){
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token){
        return extractClaims(token).get("userId", Long.class);
    }

    public List<String> extractRoles(String token){
        List<?> roles = extractClaims(token).get("roles", List.class);
        if( roles == null){
            return  Collections.emptyList();
        }

        List<String> roleName = new ArrayList<>();
        for( Object role : roles) {
            roleName.add(role.toString());
        }
        return roleName;
    }

    public boolean isTokenValid(String token){
        try {
            extractClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e){
            log.error("Invalid jwt token: {}",e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token){
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token){
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyByte = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyByte);
    }

}
