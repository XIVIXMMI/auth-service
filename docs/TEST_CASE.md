# Authentication Service - Test Cases và Test Data

## Tổng Quan

Tài liệu này mô tả chi tiết các test cases cho Authentication Service, bao gồm:
- Unit tests
- Integration tests
- Test data seeds
- Test scenarios và expected results

---

## 1. INTEGRATION TEST SUITE

### 1.1. AuthenticationIntegrationTest

**File**: `src/test/java/com/hdbank/auth_service/integration/AuthenticationIntegrationTest.java`

**Git Link**: `auth-service/src/test/java/com/hdbank/auth_service/integration/AuthenticationIntegrationTest.java`

#### Test Case 1.1: User Registration - Success

**Test Method**: `testUserRegistration()`

**Mục đích**: Verify user có thể đăng ký thành công với thông tin hợp lệ

**Test Steps**:
1. Chuẩn bị RegisterRequest với username="testuser", password="password123"
2. Gọi POST /api/v1/auth/register
3. Verify response status = 200 OK
4. Verify response body chứa thông tin user chính xác

**Request**:
```json
{
  "username": "testuser",
  "password": "password123",
  "full_name": "Test User"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Register successful",
  "data": {
    "id": 1,
    "username": "testuser",
    "full_name": "Test User",
    "enabled": true,
    "roles": ["USER"]
  }
}
```

**Assertions**:
- Status code = 200
- success = true
- data.username = "testuser"
- data.full_name = "Test User"
- data.roles is array containing "USER"

---

#### Test Case 1.2: User Registration - Invalid Username (Too Short)

**Test Method**: `testRegistrationWithInvalidUsername()`

**Mục đích**: Verify validation cho username quá ngắn

**Request**:
```json
{
  "username": "ab",
  "password": "password123",
  "full_name": "Test User"
}
```

**Expected Response**:
- Status code = 400 Bad Request
- Error message về username validation

---

#### Test Case 1.3: User Registration - Invalid Password (Too Short)

**Test Method**: `testRegistrationWithInvalidPassword()`

**Mục đích**: Verify validation cho password quá ngắn

**Request**:
```json
{
  "username": "testuser2",
  "password": "pass",
  "full_name": "Test User"
}
```

**Expected Response**:
- Status code = 400 Bad Request
- Error message về password validation

---

#### Test Case 1.4: User Login - Success

**Test Method**: `testUserLogin()`

**Mục đích**: Verify user có thể đăng nhập thành công

**Pre-conditions**:
1. Register user "loginuser" với password "password123"

**Request**:
```json
{
  "username": "loginuser",
  "password": "password123"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "550e8400-e29b-41d4-a716-446655440000",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user_info": {
      "username": "loginuser",
      "full_name": "Login User",
      "enabled": true,
      "roles": ["USER"]
    }
  }
}
```

**Assertions**:
- Status code = 200
- access_token exists và không empty
- refresh_token exists và không empty
- token_type = "Bearer"
- user_info.username = "loginuser"

---

#### Test Case 1.5: User Login - Invalid Credentials

**Test Method**: `testLoginWithInvalidCredentials()`

**Mục đích**: Verify hệ thống reject login với credentials sai

**Request**:
```json
{
  "username": "nonexistent",
  "password": "wrongpassword"
}
```

**Expected Response**:
- Status code = 401 Unauthorized
- Error message về authentication failure

---

#### Test Case 1.6: Refresh Token - Success

**Test Method**: `testRefreshToken()`

**Mục đích**: Verify có thể refresh access token thành công

**Pre-conditions**:
1. Register và login user "refreshuser"
2. Lấy refresh_token từ login response

**Request**:
```json
{
  "refresh_token": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "650e8400-e29b-41d4-a716-446655440001",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user_info": {...}
  }
}
```

**Assertions**:
- Status code = 200
- Nhận được access_token mới
- Nhận được refresh_token mới (khác với token cũ)

---

#### Test Case 1.7: Refresh Token - Invalid Token

**Test Method**: `testRefreshWithInvalidToken()`

**Mục đích**: Verify hệ thống reject refresh với invalid token

**Request**:
```json
{
  "refresh_token": "invalid-refresh-token"
}
```

**Expected Response**:
- Status code = 401 Unauthorized
- Error message về invalid refresh token

---

#### Test Case 1.8: Logout - Success

**Test Method**: `testLogout()`

**Mục đích**: Verify logout revokes refresh token thành công

**Pre-conditions**:
1. Register và login user "logoutuser"
2. Lấy access_token và refresh_token

**Request**:
```http
POST /api/v1/auth/logout
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "refresh_token": "{refresh_token}"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

**Post-conditions**:
- Thử refresh với token cũ → Nhận 401 Unauthorized

---

### 1.2. ProtectedEndpointsIntegrationTest

**File**: `src/test/java/com/hdbank/auth_service/integration/ProtectedEndpointsIntegrationTest.java`

**Git Link**: `auth-service/src/test/java/com/hdbank/auth_service/integration/ProtectedEndpointsIntegrationTest.java`

#### Test Case 2.1: Get Current User - Success

**Test Method**: `testGetCurrentUserWithValidToken()`

**Mục đích**: Verify có thể lấy thông tin user với valid JWT

**Pre-conditions**:
1. User "protecteduser" đã login
2. Có valid access_token

**Request**:
```http
GET /api/v1/auth/me
Authorization: Bearer {access_token}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "protecteduser",
    "full_name": "Protected User",
    "enabled": true,
    "roles": ["USER"]
  }
}
```

---

#### Test Case 2.2: Get Current User - No JWT

**Test Method**: `testGetCurrentUserWithoutToken()`

**Mục đích**: Verify hệ thống reject request không có token

**Request**:
```http
GET /api/v1/auth/me
```

**Expected Response**:
- Status code = 401 Unauthorized

---

#### Test Case 2.3: Get Current User - Invalid JWT

**Test Method**: `testGetCurrentUserWithInvalidToken()`

**Mục đích**: Verify hệ thống reject invalid JWT

**Request**:
```http
GET /api/v1/auth/me
Authorization: Bearer invalid.jwt.token
```

**Expected Response**:
- Status code = 401 Unauthorized

---

#### Test Case 2.4: Get Current User - Malformed Header

**Test Method**: `testGetCurrentUserWithMalformedHeader()`

**Mục đích**: Verify hệ thống reject malformed Authorization header

**Request**:
```http
GET /api/v1/auth/me
Authorization: InvalidFormat {token}
```

**Expected Response**:
- Status code = 401 Unauthorized

---

#### Test Case 2.5: Change Password - Success

**Test Method**: `testChangePasswordSuccess()`

**Mục đích**: Verify đổi password thành công với credentials hợp lệ

**Pre-conditions**:
1. User logged in với password "password123"

**Request**:
```http
PUT /api/v1/auth/change-password
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "old_password": "password123",
  "new_password": "newpassword123"
}
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null
}
```

**Post-conditions**:
- Có thể login với password mới
- Không thể login với password cũ

---

#### Test Case 2.6: Change Password - No JWT

**Test Method**: `testChangePasswordWithoutToken()`

**Mục đích**: Verify không thể đổi password mà không có JWT

**Expected Response**:
- Status code = 401 Unauthorized

---

#### Test Case 2.7: Change Password - Wrong Old Password

**Test Method**: `testChangePasswordWithWrongOldPassword()`

**Mục đích**: Verify không thể đổi password với old password sai

**Request**:
```json
{
  "old_password": "wrongoldpassword",
  "new_password": "newpassword123"
}
```

**Expected Response**:
- Status code = 400 Bad Request
- Error message về old password incorrect

---

#### Test Case 2.8: Change Password - Invalid New Password

**Test Method**: `testChangePasswordWithInvalidNewPassword()`

**Mục đích**: Verify validation cho new password quá ngắn

**Request**:
```json
{
  "old_password": "password123",
  "new_password": "short"
}
```

**Expected Response**:
- Status code = 400 Bad Request
- Error message về password validation

---

## 2. UNIT TEST SUITE

### 2.1. JwtServiceTest

**File**: `src/test/java/com/hdbank/auth_service/service/JwtServiceTest.java`

**Git Link**: `auth-service/src/test/java/com/hdbank/auth_service/service/JwtServiceTest.java`

**Test Cases**:
1. `shouldGenerateAccessToken()` - Generate JWT token
2. `shouldExtractUsername()` - Extract username từ token
3. `shouldValidateToken()` - Validate token chính xác
4. `shouldDetectExpiredToken()` - Detect token hết hạn
5. `shouldDetectInvalidToken()` - Detect token không hợp lệ

---

### 2.2. RefreshTokenServiceTest

**File**: `src/test/java/com/hdbank/auth_service/service/RefreshTokenServiceTest.java`

**Git Link**: `auth-service/src/test/java/com/hdbank/auth_service/service/RefreshTokenServiceTest.java`

**Test Cases**:
1. `shouldCreateRefreshToken()` - Tạo refresh token mới
2. `shouldVerifyValidToken()` - Verify token hợp lệ
3. `shouldRejectRevokedToken()` - Reject token đã revoke
4. `shouldRejectExpiredToken()` - Reject token hết hạn
5. `shouldRevokeToken()` - Revoke token thành công
6. `shouldRevokeAllUserTokens()` - Revoke tất cả tokens của user

---

## 3. TEST DATA SEEDS

### 3.1. Seed Data cho Development/Testing

**File**: `src/main/resources/db/migration/V8__insert_test_data.sql`

**Git Link**: `auth-service/src/main/resources/db/migration/V8__insert_test_data.sql`

```sql
-- ========================================
-- TEST DATA SEEDS
-- ========================================

-- 1. Test Roles (already created in V5)
-- USER and ADMIN roles

-- 2. Test Users
-- Password for all test users: Password@123
-- BCrypt hash: $2a$10$rIKl3Z9VvQGpKHOXZ9vJ9.VVfJPFEBZZVXKJPq8BZDj9zq7J8F0eW

-- Admin User
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'admin@hdbank.com',
    '$2a$10$rIKl3Z9VvQGpKHOXZ9vJ9.VVfJPFEBZZVXKJPq8BZDj9zq7J8F0eW',
    'System Administrator',
    true,
    'SYSTEM',
    false
);

-- Regular User 1
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'user01@hdbank.com',
    '$2a$10$rIKl3Z9VvQGpKHOXZ9vJ9.VVfJPFEBZZVXKJPq8BZDj9zq7J8F0eW',
    'Nguyen Van A',
    true,
    'SYSTEM',
    false
);

-- Regular User 2
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'user02@hdbank.com',
    '$2a$10$rIKl3Z9VvQGpKHOXZ9vJ9.VVfJPFEBZZVXKJPq8BZDj9zq7J8F0eW',
    'Tran Thi B',
    true,
    'SYSTEM',
    false
);

-- Manager User
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'manager@hdbank.com',
    '$2a$10$rIKl3Z9VvQGpKHOXZ9vJ9.VVfJPFEBZZVXKJPq8BZDj9zq7J8F0eW',
    'Le Van C',
    true,
    'SYSTEM',
    false
);

-- Disabled User (for testing)
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'disabled@hdbank.com',
    '$2a$10$rIKl3Z9VvQGpKHOXZ9vJ9.VVfJPFEBZZVXKJPq8BZDj9zq7J8F0eW',
    'Disabled User',
    false,
    'SYSTEM',
    false
);

-- 3. Assign Roles to Users

-- Admin user gets ADMIN and USER roles
INSERT INTO app_user_role (user_id, role_id, created_by, is_deleted)
SELECT u.id, r.id, 'SYSTEM', false
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username = 'admin@hdbank.com'
  AND r.name IN ('ADMIN', 'USER')
  AND u.is_deleted = false
  AND r.is_deleted = false;

-- Regular users get USER role
INSERT INTO app_user_role (user_id, role_id, created_by, is_deleted)
SELECT u.id, r.id, 'SYSTEM', false
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username IN ('user01@hdbank.com', 'user02@hdbank.com')
  AND r.name = 'USER'
  AND u.is_deleted = false
  AND r.is_deleted = false;

-- Manager gets MANAGER and USER roles
INSERT INTO app_user_role (user_id, role_id, created_by, is_deleted)
SELECT u.id, r.id, 'SYSTEM', false
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username = 'manager@hdbank.com'
  AND r.name IN ('MANAGER', 'USER')
  AND u.is_deleted = false
  AND r.is_deleted = false;

-- Disabled user gets USER role
INSERT INTO app_user_role (user_id, role_id, created_by, is_deleted)
SELECT u.id, r.id, 'SYSTEM', false
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username = 'disabled@hdbank.com'
  AND r.name = 'USER'
  AND u.is_deleted = false
  AND r.is_deleted = false;

-- 4. Sample Refresh Tokens (for testing token management)

-- Active token for user01
INSERT INTO refresh_tokens (
    user_id, token, issued_at, expires_at,
    revoked, ip_address, user_agent, created_by, is_deleted
)
SELECT
    u.id,
    '550e8400-e29b-41d4-a716-446655440001',
    NOW(),
    NOW() + INTERVAL '7 days',
    false,
    '192.168.1.100',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'user01@hdbank.com';

-- Revoked token for user01 (for testing revoked token rejection)
INSERT INTO refresh_tokens (
    user_id, token, issued_at, expires_at,
    revoked, revoked_at, ip_address, user_agent, created_by, is_deleted
)
SELECT
    u.id,
    '550e8400-e29b-41d4-a716-446655440002',
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '6 days',
    true,
    NOW() - INTERVAL '1 hour',
    '192.168.1.100',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'user01@hdbank.com';

-- Expired token for user02 (for testing expired token rejection)
INSERT INTO refresh_tokens (
    user_id, token, issued_at, expires_at,
    revoked, ip_address, user_agent, created_by, is_deleted
)
SELECT
    u.id,
    '550e8400-e29b-41d4-a716-446655440003',
    NOW() - INTERVAL '8 days',
    NOW() - INTERVAL '1 day',
    false,
    '192.168.1.101',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'user02@hdbank.com';

COMMIT;
```

---

## 4. TEST SCENARIOS BY USE CASE

### Scenario A: Đăng Ký và Đăng Nhập Lần Đầu

**Actors**: Người dùng mới

**Steps**:
1. **Register**: POST /api/v1/auth/register
   - Username: "newuser@hdbank.com"
   - Password: "SecurePass@123"
   - Expected: 200 OK, receive UserInfo

2. **Login**: POST /api/v1/auth/login
   - Username: "newuser@hdbank.com"
   - Password: "SecurePass@123"
   - Expected: 200 OK, receive tokens + UserInfo

3. **Access Protected Resource**: GET /api/v1/auth/me
   - Authorization: Bearer {access_token}
   - Expected: 200 OK, receive current user info

**Test Data**:
```json
// Register Request
{
  "username": "newuser@hdbank.com",
  "password": "SecurePass@123",
  "full_name": "New User Test"
}

// Login Request
{
  "username": "newuser@hdbank.com",
  "password": "SecurePass@123"
}
```

---

### Scenario B: Multi-Device Login

**Actors**: User đăng nhập trên nhiều thiết bị

**Steps**:
1. **Login trên Desktop**: POST /api/v1/auth/login
   - Expected: Receive tokens (T1)

2. **Login trên Mobile**: POST /api/v1/auth/login
   - Expected: Receive tokens (T2)
   - Note: Desktop refresh token (T1) bị soft delete

3. **Thử refresh trên Desktop**: POST /api/v1/auth/refresh
   - Refresh token: T1
   - Expected: 401 Unauthorized (token đã bị soft delete)

4. **Refresh trên Mobile**: POST /api/v1/auth/refresh
   - Refresh token: T2
   - Expected: 200 OK, receive new tokens (T3)

**Test Data**:
```json
// Login Request (same for both devices)
{
  "username": "user01@hdbank.com",
  "password": "Password@123"
}
```

---

### Scenario C: Đổi Mật Khẩu và Bảo Mật

**Actors**: User phát hiện tài khoản bị xâm nhập

**Steps**:
1. **Login**: POST /api/v1/auth/login
   - Expected: Receive tokens

2. **Change Password**: PUT /api/v1/auth/change-password
   - Old password: "Password@123"
   - New password: "NewSecurePass@456"
   - Expected: 200 OK, all refresh tokens revoked

3. **Thử Refresh với token cũ**: POST /api/v1/auth/refresh
   - Expected: 401 Unauthorized (token đã bị revoke)

4. **Login với password mới**: POST /api/v1/auth/login
   - Password: "NewSecurePass@456"
   - Expected: 200 OK, receive new tokens

**Test Data**:
```json
// Change Password Request
{
  "old_password": "Password@123",
  "new_password": "NewSecurePass@456"
}
```

---

### Scenario D: Token Expiration và Auto Refresh

**Actors**: Frontend application với auto-refresh logic

**Steps**:
1. **Initial Login**: Receive access_token (expires in 1 hour)

2. **Use access_token**: Gọi protected APIs
   - Expected: 200 OK trong vòng 1 giờ

3. **After 1 hour**: Access token hết hạn
   - Expected: 401 Unauthorized khi gọi protected API

4. **Auto Refresh**: Frontend tự động gọi /refresh
   - Expected: 200 OK, receive new tokens

5. **Continue using**: Sử dụng new access_token
   - Expected: 200 OK

---

## 5. PERFORMANCE TEST SCENARIOS

### 5.1. Concurrent Login Test

**Mục đích**: Test hệ thống xử lý nhiều login đồng thời

**Setup**:
- 100 users
- 10 concurrent requests/second
- Duration: 1 minute

**Expected Results**:
- All requests succeed (200 OK)
- Average response time < 500ms
- No database deadlocks
- No duplicate tokens

---

### 5.2. Token Refresh Load Test

**Mục đích**: Test khả năng xử lý token refresh đồng thời

**Setup**:
- 1000 valid refresh tokens
- 50 concurrent refresh requests/second
- Duration: 2 minutes

**Expected Results**:
- All requests succeed
- Old tokens revoked correctly
- New tokens generated uniquely
- Average response time < 300ms

---

## 6. SECURITY TEST SCENARIOS

### 6.1. Brute Force Attack Simulation

**Test**: Thử login với wrong password nhiều lần

**Expected**:
- Hệ thống không leak thông tin về existence của username
- Consistent response time (không reveal timing attack)

### 6.2. JWT Token Manipulation

**Test**: Modify JWT token payload

**Expected**:
- 401 Unauthorized
- Token signature validation fails

### 6.3. SQL Injection Test

**Test**: Username với SQL injection payload

**Expected**:
- Request blocked hoặc safely escaped
- No database errors

---

## 7. CHẠY TESTS

### Run All Tests
```bash
./gradlew test
```

### Run Integration Tests Only
```bash
./gradlew test --tests "com.hdbank.auth_service.integration.*"
```

### Run Specific Test Class
```bash
./gradlew test --tests "AuthenticationIntegrationTest"
```

### Run với Coverage Report
```bash
./gradlew test jacocoTestReport
```

**Report Location**: `build/reports/tests/test/index.html`

---

## 8. TEST DATA REFERENCE

### Default Test Users

| Username              | Password        | Roles          | Enabled | Description                    |
|-----------------------|-----------------|----------------|---------|--------------------------------|
| admin@hdbank.com      | Password@123    | ADMIN, USER    | true    | Administrator account          |
| user01@hdbank.com     | Password@123    | USER           | true    | Regular user 1                 |
| user02@hdbank.com     | Password@123    | USER           | true    | Regular user 2                 |
| manager@hdbank.com    | Password@123    | MANAGER, USER  | true    | Manager account                |
| disabled@hdbank.com   | Password@123    | USER           | false   | Disabled account for testing   |

### Test Refresh Tokens

| Token                                  | User                | Status   | Expires      |
|----------------------------------------|---------------------|----------|--------------|
| 550e8400-e29b-41d4-a716-446655440001   | user01@hdbank.com   | Active   | +7 days      |
| 550e8400-e29b-41d4-a716-446655440002   | user01@hdbank.com   | Revoked  | +6 days      |
| 550e8400-e29b-41d4-a716-446655440003   | user02@hdbank.com   | Expired  | -1 day       |

---

## 9. CONTINUOUS INTEGRATION

### GitHub Actions Workflow

```yaml
name: Run Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run tests
        run: ./gradlew test
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

## 10. TEST COVERAGE GOALS

- **Overall Coverage**: > 80%
- **Service Layer**: > 90%
- **Controller Layer**: > 85%
- **Critical Paths** (Auth, Security): 100%
