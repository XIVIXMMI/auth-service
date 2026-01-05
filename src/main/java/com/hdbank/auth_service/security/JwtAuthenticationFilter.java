package com.hdbank.auth_service.security;

import com.hdbank.auth_service.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if( authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        try {
            final String jwt = authHeader.substring(7);
            final Long userId = jwtService.extractUserId(jwt);
            final String username = jwtService.extractUsername(jwt);
            final List<String> roles = jwtService.extractRoles(jwt);

            if( username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                CustomUserDetails userDetails = CustomUserDetails.fromJwtClaims(
                        userId,
                        username,
                        roles,
                        true
                );

                if(jwtService.isTokenValid(jwt)){
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                authToken.setDetails(
                        // set detail for saving ip and session id for audit/tracking
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                    log.debug("Set authentication for user: {}", username);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request,response);
    }
}
