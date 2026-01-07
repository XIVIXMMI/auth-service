# Authentication Service - Documentation

Chào mừng đến với tài liệu của Authentication Service. Đây là microservice xử lý xác thực và phân quyền cho hệ thống HDBank.

## Cấu Trúc Tài Liệu

### [FEATURES](docs/FEATURES.md)
- Tổng quan về service
- Các chức năng chính (Register, Login, Refresh Token, Logout, etc.)
- Use cases và scenarios
- Tính năng bảo mật
- Kiến trúc kỹ thuật

### [FUNCTION_FLOWS](docs/FUNCTION_FLOWS.md)
- Kiến trúc 3-tier
- Call graphs chi tiết cho từng endpoint
- Sequence diagrams
- Flow giữa các components
- Scheduled tasks

### [DATA_DICTIONARY](docs/DATA_DICTIONARY.md)
- Entity Relationship Diagram (ERD)
- Chi tiết từng bảng trong database
- Relationships giữa các bảng
- Indexes và constraints
- Query patterns và performance considerations

### [TEST_CASE](docs/TEST_CASE.md)
- Integration test suites
- Unit test suites
- Test scenarios theo use case
- Test data seeds
- Performance và security test scenarios

---

## Quick Start

### Prerequisites
- Java 21
- PostgreSQL 14+
- Gradle 8.14+

### Setup Database

1. Tạo database:
```sql
CREATE DATABASE auth_service;
```

2. Flyway sẽ tự động chạy migrations khi start application

3. Load test data (optional):
```bash
# Test data sẽ được load tự động từ V8__insert_test_data.sql
```

### Run Application

```bash
# Development mode
./gradlew bootRun --args='--spring.profiles.active=local'

# Build
./gradlew build

# Run tests
./gradlew test

# Run integration tests only
./gradlew test --tests "com.hdbank.auth_service.integration.*"
```

---

## Test Credentials

Sau khi run migration V8, bạn có thể sử dụng các test accounts sau:

| Username              | Password      | Roles          | Status   |
|-----------------------|---------------|----------------|----------|
| admin@hdbank.com      | Password@123  | ADMIN, USER    | Enabled  |
| user01@hdbank.com     | Password@123  | USER           | Enabled  |
| user02@hdbank.com     | Password@123  | USER           | Enabled  |
| manager@hdbank.com    | Password@123  | MANAGER, USER  | Enabled  |
| disabled@hdbank.com   | Password@123  | USER           | Disabled |
| testuser@hdbank.com   | Password@123  | USER           | Enabled  |

---

## API Endpoints

### Public Endpoints (No Authentication)
```
POST   /api/v1/auth/register       - Đăng ký user mới
POST   /api/v1/auth/login          - Đăng nhập
POST   /api/v1/auth/refresh        - Refresh access token
```

### Protected Endpoints (Require JWT)
```
POST   /api/v1/auth/logout         - Đăng xuất
GET    /api/v1/auth/me             - Lấy thông tin user hiện tại
PUT    /api/v1/auth/change-password - Đổi mật khẩu
```

### API Documentation
Sau khi start application, truy cập:
```
http://localhost:8080/swagger-ui.html
```

---

## Testing

### Test Structure

```
src/test/java/
├── com.hdbank.auth_service/
│   ├── integration/              # Integration tests
│   │   ├── AuthenticationIntegrationTest.java
│   │   └── ProtectedEndpointsIntegrationTest.java
│   └── service/                  # Unit tests
│       ├── JwtServiceTest.java
│       └── RefreshTokenServiceTest.java
```

### Run Tests

```bash
# All tests
./gradlew test

# With coverage report
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html

# Specific test class
./gradlew test --tests "AuthenticationIntegrationTest"

# Clean and test
./gradlew clean test
```

### Test Reports
- Test results: `build/reports/tests/test/index.html`
- Coverage report: `build/reports/jacoco/test/html/index.html`

---

## Database Migrations

Migrations được quản lý bởi Flyway:

```
src/main/resources/db/migration/
├── V1__create_app_users_table.sql
├── V2__create_app_roles_table.sql
├── V3__create_app_users_roles_table.sql
├── V4__create_refresh_tokens_table.sql
├── V5__insert_value_app_roles.sql
├── V6__remove_audit_column.sql
├── V7__insert_test_data.sql
└── V8__insert_more_test_data.sql        # Test data seeds
```

### Migration Commands

```bash
# Info về migrations
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Migrate database
./gradlew flywayMigrate

# Clean database (CẢNH BÁO: Xóa tất cả data)
./gradlew flywayClean
```

---

## Security Features

### JWT Configuration
- Algorithm: HS256
- Access Token Expiration: 1 hour (configurable)
- Refresh Token Expiration: 7 days (configurable)

### Password Security
- BCrypt hashing with cost factor 10
- Minimum length: 8 characters
- No plaintext storage

### Token Management
- Refresh token rotation on refresh
- Automatic soft delete of old tokens
- Multi-device login support
- Revoke all tokens on password change

### Audit Trail
- All entities track: created_by, created_at, updated_by, updated_at
- Soft delete with deleted_by, deleted_at
- Immutable audit fields

---

## Architecture

### Layers
```
Controller Layer
    ↓
Service Layer
    ↓
Repository Layer
    ↓
Database
```

### Key Components

**Controllers**:
- `AuthController` - Authentication endpoints

**Services**:
- `AuthService` - Authentication business logic
- `UserService` - User management
- `RefreshTokenService` - Token lifecycle
- `JwtService` - JWT operations
- `CustomUserDetailsService` - Spring Security integration

**Repositories**:
- `AppUserRepository` - User CRUD
- `AppRoleRepository` - Role CRUD
- `RefreshTokenRepository` - Token CRUD + custom queries

**Security**:
- `JwtAuthenticationFilter` - JWT validation filter
- `SecurityConfig` - Spring Security configuration
- `JwtAuthenticationEntryPoint` - Auth error handler

---

## Database Schema

```
app_users ←──┐
    ↑        │
    │        │ (Many-to-Many)
    │        │
    │    app_user_role
    │        │
    │        │
    ↓        │
app_roles ←──┘

app_users ←── refresh_tokens (One-to-Many)
```

Chi tiết xem [DATA_DICTIONARY.md](docs/DATA_DICTIONARY.md)

---

## Common Workflows

### 1. User Registration Flow
```
Client → POST /register
    → AuthController.register()
        → AuthService.register()
            → Check username exists
            → Hash password
            → Assign USER role
            → Save to database
        → Return UserInfo
```

### 2. Login Flow
```
Client → POST /login
    → AuthController.login()
        → AuthService.login()
            → Authenticate credentials
            → Generate JWT access token
            → Create refresh token
            → Soft delete old refresh tokens
        → Return tokens + UserInfo
```

### 3. Refresh Token Flow
```
Client → POST /refresh
    → AuthController.refreshToken()
        → AuthService.refreshToken()
            → Verify refresh token
            → Revoke old token
            → Generate new tokens
        → Return new tokens + UserInfo
```

Chi tiết xem [FUNCTION_FLOWS.md](docs/FUNCTION_FLOWS.md)

---

## Configuration

### application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/auth_service
spring.datasource.username=postgres
spring.datasource.password=your_password

# JWT
jwt.secret=your-secret-key-minimum-256-bits-long
jwt.expiration=3600000          # 1 hour in ms
jwt.refresh-expiration=604800000 # 7 days in ms

# Server
server.port=8080
```

### application-local.properties

Local development overrides (gitignored)

---

## Troubleshooting

### Common Issues

**1. Database connection error**
```bash
# Check PostgreSQL is running
brew services list | grep postgresql

# Start PostgreSQL
brew services start postgresql
```

**2. Migration error**
```bash
# Check migration status
./gradlew flywayInfo

# Repair if needed
./gradlew flywayRepair
```

**3. Test failures**
```bash
# Clean build and retry
./gradlew clean test --rerun-tasks
```

**4. JWT token invalid**
- Check jwt.secret is configured
- Verify token hasn't expired
- Check system time is synchronized

---

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/) - JWT debugger
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

## Contributing

### Code Style
- Follow Java naming conventions
- Use Lombok annotations
- Write tests for new features
- Update documentation

### Git Workflow
```bash
# Create feature branch
git checkout -b feature/your-feature

# Make changes and test
./gradlew test

# Commit with meaningful message
git commit -m "feat: add new feature"

# Push and create PR
git push origin feature/your-feature
```

---
