# Authentication Service - Function Flow và Call Graph

## Tổng Quan Kiến Trúc

Hệ thống theo kiến trúc 3 lớp (3-tier architecture):

```
┌─────────────────────────────────────────────────────┐
│              CONTROLLER LAYER                       │
│  - AuthController                                   │
│  - Xử lý HTTP requests/responses                    │
│  - Validation đầu vào                               │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│              SERVICE LAYER                          │
│  - AuthService                                      │
│  - UserService                                      │
│  - RefreshTokenService                              │
│  - JwtService                                       │
│  - Business logic & transactions                    │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│              REPOSITORY LAYER                       │
│  - AppUserRepository                                │
│  - AppRoleRepository                                │
│  - RefreshTokenRepository                           │
│  - Database access                                  │
└─────────────────────────────────────────────────────┘
```

---

## 1. USER REGISTRATION FLOW

### Endpoint: `POST /api/v1/auth/register`

### Call Graph

```
AuthController.register()
    │
    ├─> Validate RegisterRequest (@Valid annotation)
    │   ├─> Username: 3-100 chars
    │   └─> Password: min 8 chars
    │
    └─> AuthService.register(RegisterRequest)
        │
        ├─> AppUserRepository.existsByUsernameAndDeletedFalse(username)
        │   └─> Check nếu username đã tồn tại
        │       └─> [YES] throw UserAlreadyExistsException
        │
        ├─> AppRoleRepository.findByNameAndDeletedFalse("USER")
        │   └─> Lấy default role
        │       └─> [NOT FOUND] throw RoleNotFoundException
        │
        ├─> PasswordEncoder.encode(password)
        │   └─> BCrypt hash password
        │
        ├─> AppUser.builder()
        │   ├─> Set username
        │   ├─> Set passwordHash
        │   ├─> Set fullName
        │   ├─> Set enabled = true
        │   └─> Set roles = [userRole]
        │
        ├─> AppUserRepository.save(newUser)
        │   └─> INSERT vào database
        │
        └─> Build UserInfo response
            ├─> Map từ AppUser entity
            └─> Return UserInfo
```

### Sequence Diagram

```
Client          Controller         Service            Repository        Database
  │                 │                 │                    │                │
  ├─Register───────>│                 │                    │                │
  │  Request        │                 │                    │                │
  │                 │                 │                    │                │
  │                 ├─Validate───────>│                    │                │
  │                 │  Input          │                    │                │
  │                 │                 │                    │                │
  │                 ├─register()─────>│                    │                │
  │                 │                 │                    │                │
  │                 │                 ├─Check username────>│                │
  │                 │                 │  exists?           ├─SELECT────────>│
  │                 │                 │                    │<───────────────┤
  │                 │                 │<───────────────────┤                │
  │                 │                 │                    │                │
  │                 │                 ├─Find USER role───> │                │ 
  │                 │                 │                    ├─SELECT────────>│
  │                 │                 │                    │<───────────────┤
  │                 │                 │<───────────────────┤                │
  │                 │                 │                    │                │
  │                 │                 ├─Hash password      │                │
  │                 │                 │  (BCrypt)          │                │
  │                 │                 │                    │                │
  │                 │                 ├─Save user─────────>│                │
  │                 │                 │                    ├─INSERT────────>│
  │                 │                 │                    │<───────────────┤
  │                 │                 │<───────────────────┤                │
  │                 │                 │                    │                │
  │                 │<─UserInfo───────┤                    │                │
  │                 │                 │                    │                │
  │<─200 OK─────────┤                 │                    │                │
  │  UserInfo       │                 │                    │                │
```

---

## 2. USER LOGIN FLOW

### Endpoint: `POST /api/v1/auth/login`

### Call Graph

```
AuthController.login(LoginRequest, HttpServletRequest)
    │
    ├─> Extract IP address
    │   └─> getClientIP(HttpServletRequest)
    │       ├─> Check X-Forwarded-For header
    │       └─> Fallback to remoteAddr
    │
    ├─> Extract User-Agent header
    │
    └─> AuthService.login(LoginRequest, ipAddress, userAgent)
        │
        ├─> AuthenticationManager.authenticate()
        │   └─> UsernamePasswordAuthenticationToken
        │       │
        │       └─> CustomUserDetailsService.loadUserByUsername()
        │           │
        │           ├─> AppUserRepository.findByUsernameAndDeletedFalse()
        │           │   └─> [NOT FOUND] throw UsernameNotFoundException
        │           │
        │           └─> Build CustomUserDetails
        │               ├─> Map username
        │               ├─> Map password
        │               └─> Map authorities from roles
        │
        ├─> PasswordEncoder.matches()
        │   └─> Verify password
        │       └─> [FAILED] throw BadCredentialsException
        │
        ├─> AppUserRepository.findByUsernameAndDeletedFalse(username)
        │   └─> Load user entity
        │
        ├─> JwtService.generateAccessToken(user)
        │   │
        │   ├─> Claims.builder()
        │   │   ├─> Set subject (username)
        │   │   ├─> Set issuedAt (now)
        │   │   ├─> Set expiration (now + jwtExpiration)
        │   │   └─> Set roles claim
        │   │
        │   └─> Jwts.builder().signWith(secretKey)
        │       └─> Generate JWT token
        │
        ├─> RefreshTokenService.createRefreshToken(user, ipAddress, userAgent)
        │   │
        │   ├─> RefreshTokenRepository.softDeleteByUserId(userId)
        │   │   └─> UPDATE old tokens: set is_deleted=true
        │   │
        │   ├─> RefreshToken.builder()
        │   │   ├─> Set user
        │   │   ├─> Set token = UUID.randomUUID()
        │   │   ├─> Set issuedAt = now
        │   │   ├─> Set expiresAt = now + refreshExpiration
        │   │   ├─> Set ipAddress
        │   │   ├─> Set userAgent
        │   │   └─> Set revoked = false
        │   │
        │   └─> RefreshTokenRepository.save(refreshToken)
        │       └─> INSERT new refresh token
        │
        └─> Build LoginResponse
            ├─> Set accessToken
            ├─> Set refreshToken
            ├─> Set tokenType = "Bearer"
            ├─> Set expiresIn
            └─> Set userInfo
```

### Sequence Diagram

```
Client      Controller    AuthManager    UserDetails    JwtService    RefreshToken    Database
  │             │              │             Service         │           Service          │
  ├─Login──────>│              │               │             │              │             │
  │  Request    │              │               │             │              │             │
  │             │              │               │             │              │             │
  │             ├─Extract IP   │               │             │              │             │
  │             ├─Extract UA   │               │             │              │             │
  │             │              │               │             │              │             │
  │             ├─login()─────>│               │             │              │             │
  │             │              │               │             │              │             │
  │             │              ├authenticate()>│             │              │             │
  │             │              │               │             │              │             │
  │             │              │               ├─load user──>│              │             │
  │             │              │               │             │              ├─SELECT─────>│
  │             │              │               │             │              │<────────────┤
  │             │              │               │<────────────┤              │             │
  │             │              │               │             │              │             │
  │             │              │               ├─verify pwd  │              │             │
  │             │              │<──────────────┤             │              │             │
  │             │              │               │             │              │             │
  │             │<─────────────┤               │             │              │             │
  │             │              │               │             │              │             │
  │             ├─generate JWT──────────────────────────────>│              │             │
  │             │              │               │             │              │             │
  │             │<───────────────────────────────────────────┤              │             │
  │             │              │               │             │              │             │
  │             ├─create refresh token─────────────────────────────────────>│             │
  │             │              │               │             │              │             │
  │             │              │               │             │              ├─soft delete>│
  │             │              │               │             │              │  old tokens │
  │             │              │               │             │              ├─INSERT new─>│
  │             │              │               │             │              │<────────────┤
  │             │<──────────────────────────────────────────────────────────┤             │
  │             │              │               │             │              │             │
  │<─200 OK─────┤              │               │             │              │             │
  │  Tokens +   │              │               │             │              │             │
  │  UserInfo   │              │               │             │              │             │
```

---

## 3. REFRESH TOKEN FLOW

### Endpoint: `POST /api/v1/auth/refresh`

### Call Graph

```
AuthController.refreshToken(RefreshTokenRequest, HttpServletRequest)
    │
    ├─> Extract IP address
    ├─> Extract User-Agent
    │
    └─> AuthService.refreshToken(refreshToken, ipAddress, userAgent)
        │
        ├─> RefreshTokenService.verifyRefreshToken(refreshTokenStr)
        │   │
        │   ├─> RefreshTokenRepository.findByTokenAndDeletedFalse(token)
        │   │   └─> [NOT FOUND] throw InvalidRefreshTokenException
        │   │
        │   ├─> Check if revoked
        │   │   └─> [YES] throw InvalidRefreshTokenException
        │   │
        │   ├─> Check if expired
        │   │   └─> [YES] throw InvalidRefreshTokenException
        │   │
        │   └─> Return valid RefreshToken entity
        │
        ├─> Get user from refresh token
        │   └─> refreshToken.getUser()
        │
        ├─> RefreshTokenService.revokeRefreshToken(refreshTokenStr)
        │   │
        │   ├─> RefreshTokenRepository.findByTokenAndDeletedFalse(token)
        │   │
        │   ├─> Set revoked = true
        │   ├─> Set revokedAt = now
        │   │
        │   └─> RefreshTokenRepository.save(refreshToken)
        │       └─> UPDATE refresh_tokens
        │
        ├─> JwtService.generateAccessToken(user)
        │   └─> Generate new JWT token
        │
        ├─> RefreshTokenService.createRefreshToken(user, ipAddress, userAgent)
        │   └─> Create new refresh token
        │
        └─> Build LoginResponse
            └─> Return new tokens + user info
```

---

## 4. LOGOUT FLOW

### Endpoint: `POST /api/v1/auth/logout`

### Call Graph

```
AuthController.logout(RefreshTokenRequest)
    │
    ├─> JwtAuthenticationFilter validates JWT
    │   └─> Extract username from token
    │
    └─> AuthService.logout(refreshToken)
        │
        └─> RefreshTokenService.revokeRefreshToken(refreshToken)
            │
            ├─> RefreshTokenRepository.findByTokenAndDeletedFalse(token)
            │   └─> [NOT FOUND] throw InvalidRefreshTokenException
            │
            ├─> Set revoked = true
            ├─> Set revokedAt = now
            │
            └─> RefreshTokenRepository.save(refreshToken)
                └─> UPDATE refresh_tokens
```

---

## 5. GET CURRENT USER FLOW

### Endpoint: `GET /api/v1/auth/me`

### Call Graph

```
AuthController.getCurrentUser(Authentication)
    │
    ├─> Extract username from Authentication
    │   └─> authentication.getName()
    │
    └─> UserService.getUserInfo(username)
        │
        ├─> UserService.findByUserName(username)
        │   │
        │   └─> AppUserRepository.findByUsernameAndDeletedFalse(username)
        │       └─> [NOT FOUND] throw UsernameNotFoundException
        │
        └─> Build UserInfo
            ├─> Map user properties
            ├─> Extract role names from roles collection
            └─> Return UserInfo
```

---

## 6. CHANGE PASSWORD FLOW

### Endpoint: `PUT /api/v1/auth/change-password`

### Call Graph

```
AuthController.changePassword(ChangePasswordRequest, Authentication)
    │
    ├─> Extract username from Authentication
    │
    └─> UserService.changePassword(username, request)
        │
        ├─> UserService.findByUserName(username)
        │   └─> Load user entity
        │
        ├─> PasswordEncoder.matches(oldPassword, user.passwordHash)
        │   └─> [FAILED] throw InvalidPasswordException
        │
        ├─> PasswordEncoder.encode(newPassword)
        │   └─> Hash new password
        │
        ├─> user.setPasswordHash(newPasswordHash)
        │
        ├─> AppUserRepository.save(user)
        │   └─> UPDATE app_users
        │
        └─> RefreshTokenService.revokeAllByUserId(userId)
            │
            ├─> RefreshTokenRepository.findByUser_IdAndDeletedFalse(userId)
            │   └─> Get all active refresh tokens
            │
            ├─> For each token:
            │   ├─> Set revoked = true
            │   └─> Set revokedAt = now
            │
            ├─> RefreshTokenRepository.saveAll(tokens)
            │   └─> UPDATE all tokens
            │
            └─> RefreshTokenRepository.softDeleteByUserId(userId)
                └─> UPDATE tokens: set is_deleted=true
```

---

## 7. JWT AUTHENTICATION FILTER

### Flow tự động cho mọi protected endpoint

```
JwtAuthenticationFilter.doFilterInternal(request, response, chain)
    │
    ├─> Extract Authorization header
    │   └─> [MISSING] continue filter chain
    │
    ├─> Check "Bearer " prefix
    │   └─> [INVALID] continue filter chain
    │
    ├─> Extract JWT token
    │
    ├─> JwtService.extractUsername(token)
    │   │
    │   ├─> Jwts.parser()
    │   ├─> Validate signature
    │   ├─> Check expiration
    │   └─> Extract subject (username)
    │
    ├─> Check if not already authenticated
    │
    ├─> CustomUserDetailsService.loadUserByUsername(username)
    │   └─> Load user details
    │
    ├─> JwtService.isTokenValid(token, userDetails)
    │   │
    │   ├─> Extract username from token
    │   ├─> Compare with userDetails.username
    │   └─> Check if not expired
    │
    ├─> [VALID] Create UsernamePasswordAuthenticationToken
    │   └─> Set in SecurityContextHolder
    │
    └─> Continue filter chain
```

---

## 8. SCHEDULED TASKS

### Clean Up Expired Tokens

```
ScheduledTask.cleanUpExpiredRefreshTokens()
    │
    └─> RefreshTokenService.deleteExpiredTokens()
        │
        └─> RefreshTokenRepository.softDeleteExpiredTokens(now)
            └─> UPDATE refresh_tokens
                SET is_deleted = true,
                    deleted_at = now,
                    deleted_by = 'SYSTEM'
                WHERE expires_at < now
                  AND is_deleted = false
```

**Cron Expression**: Chạy hàng ngày lúc 2:00 AM

---

## Key Components Summary

### Controllers
- **AuthController**: Xử lý tất cả endpoints liên quan đến authentication

### Services
- **AuthService**: Business logic cho register, login, refresh, logout
- **UserService**: User management và change password
- **RefreshTokenService**: Quản lý lifecycle của refresh tokens
- **JwtService**: Generate và validate JWT tokens
- **CustomUserDetailsService**: Load user cho Spring Security

### Repositories
- **AppUserRepository**: CRUD operations cho users
- **AppRoleRepository**: CRUD operations cho roles
- **RefreshTokenRepository**: CRUD operations + custom queries cho refresh tokens

### Security Components
- **JwtAuthenticationFilter**: Filter xác thực JWT cho mọi request
- **JwtAuthenticationEntryPoint**: Handler cho authentication errors
- **SecurityConfig**: Configuration cho Spring Security
- **PasswordEncoder**: BCrypt encoder cho passwords
