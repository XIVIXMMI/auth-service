# Auth Service

> **Authentication & Authorization Microservice** 

A production-ready Spring Boot microservice that provides JWT-based authentication, user management, and role-based authorization for the HDBank microservices ecosystem.

---

## Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Configuration](#-configuration)
- [Testing](#-testing)
- [Security](#-security)
- [Project Structure](#-project-structure)

---

## Features

### Core Functionality
- **User Registration & Authentication** - Secure user signup and login with BCrypt password hashing
- **JWT Token Management** - Access token (15 min) + Refresh token (7 days) pattern
- **Role-Based Access Control (RBAC)** - Multi-role support with Spring Security
- **Password Management** - Secure password change with old password validation
- **Token Refresh** - Seamless access token renewal without re-login
- **Logout & Token Revocation** - Immediate token invalidation
- **User Profile** - Retrieve authenticated user information

### Security Features
- **BCrypt Password Encryption** - Industry-standard password hashing (strength 12)
- **JWT Signature Verification** - HS256 algorithm with secret key
- **Refresh Token Tracking** - IP address and user agent monitoring
- **Soft Delete Pattern** - Audit trail compliance (7-year retention)
- **Input Validation** - Jakarta Bean Validation with custom constraints
- **CORS Configuration** - Controlled cross-origin resource sharing

### Operational Features
- **Swagger/OpenAPI 3.0** - Interactive API documentation
- **Database Migration** - Flyway versioned schema management
- **Audit Logging** - Created/Updated/Deleted by & timestamp tracking
- **Connection Pooling** - HikariCP optimized database connections
- **Health Checks** - Spring Boot Actuator endpoints

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Framework** | Spring Boot | 3.5.9 |
| **Language** | Java | 21 (LTS) |
| **Security** | Spring Security 6 | Latest |
| **JWT Library** | JJWT | 0.12.3 |
| **Database** | PostgreSQL | 16+ |
| **Migration** | Flyway | Latest |
| **ORM** | Spring Data JPA / Hibernate | Latest |
| **Validation** | Jakarta Validation | Latest |
| **Documentation** | SpringDoc OpenAPI | 2.7.0 |
| **Build Tool** | Gradle | 8.x |
| **Testing** | JUnit 5, Mockito, H2 | Latest |

---

## Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────┐
│          REST API Layer                     │
│  (AuthController - JWT Authentication)      │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Service Layer                       │
│  (AuthService, UserService, JwtService)     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│       Repository Layer                      │
│  (Spring Data JPA Repositories)             │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         PostgreSQL Database                 │
│  (4 tables: users, roles, user_role, tokens)│
└─────────────────────────────────────────────┘
```

### Security Flow

```
User Login
    ↓
AuthController.login()
    ↓
AuthService.authenticate()
    ↓
Spring Security (BCrypt verification)
    ↓
JwtService.generateAccessToken() + RefreshTokenService.createRefreshToken()
    ↓
Return: { accessToken, refreshToken, expiresIn }
```

---

## Getting Started

### Prerequisites

- **Java 21** or higher ([Download](https://adoptium.net/))
- **PostgreSQL 16** running on `localhost:25433`
- **Gradle 8.x** (or use wrapper `./gradlew`)
- **Docker** (optional, for database)

### 1. Clone the Repository

```bash
git clone <repository-url>
cd auth-service
```

### 2. Database Setup

**Option A: Using Docker (Recommended)**
```bash
# Start PostgreSQL container
docker run -d \
  --name auth-db \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=123456 \
  -p 25433:5432 \
  postgres:16
```

**Option B: Local PostgreSQL**
```sql
CREATE DATABASE auth_db;
CREATE USER postgres WITH PASSWORD '123456';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
```

### 3. Configure Application

Create `src/main/resources/application-local.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:25433/auth_db
spring.datasource.username=postgres
spring.datasource.password=123456

# JWT
jwt.secret=your-256-bit-secret-key-change-this-in-production-min-32-chars
jwt.expiration=900000          # 15 minutes
jwt.refresh-expiration=604800000  # 7 days

# Server
server.port=8081
```

### 4. Build & Run

```bash
# Build project
./gradlew build

# Run application
./gradlew bootRun --args='--spring.profiles.active=local'

# Or run the JAR
java -jar build/libs/auth-service-0.0.1-SNAPSHOT.jar
```

### 5. Verify Installation

```bash
# Check health
curl http://localhost:8081/actuator/health

# Access Swagger UI
open http://localhost:8081/swagger-ui.html
```

---

## API Documentation

### Swagger UI
Access interactive API documentation at: **http://localhost:8081/swagger-ui.html**

### OpenAPI JSON
Raw OpenAPI spec: **http://localhost:8081/v3/api-docs**

### Available Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/v1/auth/register` | Register new user | No            |
| `POST` | `/api/v1/auth/login` | Authenticate user | No            |
| `POST` | `/api/v1/auth/refresh` | Refresh access token | No            |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token | Yes           |
| `GET` | `/api/v1/auth/me` | Get current user info | Yess          |
| `PUT` | `/api/v1/auth/change-password` | Change password | Yes           |

### Example API Calls

**Register User**
```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123",
    "fullName": "John Doe"
  }'
```

**Login**
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenType": "Bearer",
    "expiresIn": 900000
  }
}
```

**Access Protected Endpoint**
```bash
curl -X GET http://localhost:8081/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🗄Database Schema

### Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│  app_users   │       │  app_user_role   │       │  app_roles   │
├──────────────┤       ├──────────────────┤       ├──────────────┤
│ id (PK)      │───┐   │ user_id (FK)     │   ┌───│ id (PK)      │
│ username     │   └───│ role_id (FK)     │───┘   │ name         │
│ password_hash│       └──────────────────┘       │ description  │
│ full_name    │                                  └──────────────┘
│ enabled      │
│ created_at   │       ┌──────────────────┐
│ updated_at   │       │ refresh_tokens   │
│ deleted_at   │       ├──────────────────┤
│ created_by   │       │ id (PK)          │
│ updated_by   │       │ user_id (FK)     │───┐
│ deleted_by   │       │ token (UUID)     │   │
│ is_deleted   │       │ issued_at        │   │
└──────────────┘       │ expires_at       │   │
                       │ revoked          │   │
                       │ revoked_at       │   │
                       │ ip_address       │   │
                       │ user_agent       │   │
                       └──────────────────┘   │
                                              │
                       ┌──────────────────────┘
```

### Migrations (Flyway)

Migrations are located in `src/main/resources/db/migration/`:

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__create_app_users_table.sql` | Users table with audit fields |
| V2 | `V2__create_app_roles_table.sql` | Roles table |
| V3 | `V3__create_app_users_roles_table.sql` | Many-to-many join table |
| V4 | `V4__create_refresh_tokens_table.sql` | Refresh tokens tracking |
| V5 | `V5__insert_value_app_roles.sql` | Seed default roles (USER, ADMIN) |
| V6 | `V6__remove_audit_column.sql` | Remove redundant audit from join table |
| V7 | `V7__insert_test_data.sql` | Test users for development |

### Test Data (V7 Migration)

| Username | Password | Roles | Enabled |
|----------|----------|-------|---------|
| `admin` | `Password123` | USER, ADMIN | yes     |
| `test.user` | `Password123` | USER | yes     |
| `john.doe` | `Password123` | USER | yes     |
| `jane.smith` | `Password123` | USER | yes     |
| `disabled.user` | `Password123` | USER | no      |

---

## ⚙️ Configuration

### Environment Variables

For production, use environment variables instead of hardcoded values:

```bash
export DATABASE_URL=jdbc:postgresql://prod-db:5432/auth_db
export DATABASE_USERNAME=auth_user
export DATABASE_PASSWORD=<secure-password>
export SECRET_KEY=<256-bit-random-key>
export EXPIRATION=900000
export REFRESH_EXPIRATION=604800000
export SERVER_PORT=8081
```

### Application Profiles

- **`local`** - Development with local PostgreSQL
- **`test`** - Testing with H2 in-memory database
- **`prod`** - Production configuration (use env vars)

Activate profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### JWT Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `jwt.secret` | (required) | HMAC secret key (min 256 bits) |
| `jwt.expiration` | `900000` | Access token TTL (15 min) |
| `jwt.refresh-expiration` | `604800000` | Refresh token TTL (7 days) |

### Database Connection Pool (HikariCP)

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Testing

### Run All Tests

```bash
./gradlew test
```

### Test Coverage

- **Unit Tests**: Service layer business logic
  - `JwtServiceTest` - Token generation & validation
  - `RefreshTokenServiceTest` - Token lifecycle management
  - `AuthServiceTest` - Authentication workflows

- **Integration Tests**: API endpoints with H2 database
  - `AuthControllerIntegrationTest` - End-to-end API testing

### Test Database

Tests use **H2 in-memory database** configured in `application-test.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

### Run Specific Test

```bash
./gradlew test --tests RefreshTokenServiceTest
./gradlew test --tests AuthControllerIntegrationTest
```

---

## Security

### Authentication Flow

1. **User Registration**
   - Password hashed with BCrypt (strength 12)
   - Default role `USER` assigned
   - Audit fields populated

2. **User Login**
   - Credentials validated via Spring Security
   - Access token (JWT) generated with claims: `sub`, `roles`, `iat`, `exp`
   - Refresh token (UUID) stored in database with IP + User Agent

3. **Token Refresh**
   - Validate refresh token (not expired, not revoked)
   - Generate new access token
   - Optionally rotate refresh token

4. **Logout**
   - Revoke refresh token in database
   - Access token remains valid until expiration (stateless)

### Password Policy

- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- Validated via `@Pattern` annotation

### JWT Claims

```json
{
  "sub": "john.doe",
  "roles": ["USER", "ADMIN"],
  "iat": 1704470400,
  "exp": 1704474000
}
```

### Security Best Practices

 **Implemented:**
- BCrypt password hashing
- JWT signature verification
- Refresh token rotation
- Soft delete for audit trail
- Input validation
- CORS configuration
- SQL injection prevention (JPA parameterized queries)

 **Recommendations for Production:**
- Enable HTTPS only
- Use HttpOnly cookies for tokens
- Implement rate limiting (e.g., Bucket4j)
- Add CAPTCHA after failed login attempts
- Enable Spring Security CSRF protection
- Rotate JWT secret key periodically
- Implement MFA (Multi-Factor Authentication)

---

## Project Structure

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/hdbank/auth_service/
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java          # Swagger configuration
│   │   │   │   ├── ScheduledTask.java          # Cleanup expired tokens
│   │   │   │   └── SecurityConfig.java         # Spring Security config
│   │   │   ├── controller/
│   │   │   │   └── AuthController.java         # REST API endpoints
│   │   │   ├── dto/
│   │   │   │   ├── request/                    # Request DTOs
│   │   │   │   │   ├── ChangePasswordRequest.java
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   └── RegisterRequest.java
│   │   │   │   └── response/                   # Response DTOs
│   │   │   │       ├── ApiDataResponse.java
│   │   │   │       ├── LoginResponse.java
│   │   │   │       └── RegisterResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── BaseEntity.java             # Audit fields
│   │   │   │   ├── AppUser.java                # User entity
│   │   │   │   ├── AppRole.java                # Role entity
│   │   │   │   └── RefreshToken.java           # Token entity
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java # Centralized error handling
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── InvalidPasswordException.java
│   │   │   │   ├── InvalidRefreshTokenException.java
│   │   │   │   ├── RoleNotFoundException.java
│   │   │   │   └── UserAlreadyExistsException.java
│   │   │   ├── repository/
│   │   │   │   ├── AppUserRepository.java
│   │   │   │   ├── AppRoleRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java    # JWT validation filter
│   │   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java            # Authentication logic
│   │   │   │   ├── JwtService.java             # JWT generation/validation
│   │   │   │   ├── RefreshTokenService.java    # Token lifecycle
│   │   │   │   └── UserService.java            # User management
│   │   │   └── AuthServiceApplication.java     # Main application
│   │   └── resources/
│   │       ├── db/migration/                   # Flyway migrations
│   │       │   ├── V1__create_app_users_table.sql
│   │       │   ├── V2__create_app_roles_table.sql
│   │       │   ├── V3__create_app_users_roles_table.sql
│   │       │   ├── V4__create_refresh_tokens_table.sql
│   │       │   ├── V5__insert_value_app_roles.sql
│   │       │   ├── V6__remove_audit_column.sql
│   │       │   └── V7__insert_test_data.sql
│   │       ├── application.properties          # Main config
│   │       └── application-local.properties    # Local dev config
│   └── test/
│       └── java/com/hdbank/auth_service/
│           ├── service/
│           │   ├── JwtServiceTest.java
│           │   └── RefreshTokenServiceTest.java
│           └── AuthServiceApplicationTests.java
├── build.gradle                                # Gradle build config
├── settings.gradle
└── README.md
```

---

##  Contributing

### Code Style

- Follow **Google Java Style Guide**
- Use explicit imports (no wildcards)
- Maximum line length: 120 characters
- Use Lombok annotations to reduce boilerplate

### Git Workflow

1. Create feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -m "feat: add new feature"`
3. Run tests: `./gradlew test`
4. Push to remote: `git push origin feature/your-feature`
5. Create Pull Request

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation only
- `refactor:` - Code refactoring
- `test:` - Adding tests
- `chore:` - Maintenance tasks

---
## Roadmap

### Version 1.0 (Current)
-  User registration & authentication
-  JWT token management
-  Role-based access control
-  Password change functionality
-  Swagger documentation

### Version 1.1 (Planned)
-  Multi-factor authentication (MFA)
-  Account lockout after failed attempts
-  Email verification
-  Password reset flow
-  OAuth2 integration (Google, Facebook)

### Version 2.0 (Future)
-   Redis caching for performance
-   Kafka event streaming
-   Audit log microservice integration
-   GraphQL API support

---
