# Authentication Service - Data Dictionary và Mô Hình Quan Hệ

## Mô Hình Quan Hệ (ERD)

```
┌─────────────────────────────────────────────────────────────────┐
│                         app_users                               │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id              BIGSERIAL                                  │
│    │ username        VARCHAR(100)  UNIQUE NOT NULL              │
│    │ password_hash   VARCHAR(255)  NOT NULL                     │
│    │ full_name       VARCHAR(255)                               │
│    │ enabled         BOOLEAN       NOT NULL DEFAULT TRUE        │
│    │ created_at      TIMESTAMPTZ   NOT NULL                     │
│    │ updated_at      TIMESTAMPTZ                                │
│    │ deleted_at      TIMESTAMPTZ                                │
│    │ created_by      VARCHAR(100)  NOT NULL                     │
│    │ updated_by      VARCHAR(100)                               │
│    │ deleted_by      VARCHAR(100)                               │
│    │ is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE       │
└──────────────┬──────────────────────────────────────────────────┘
               │
               │ 1
               │
               │ N (Many-to-Many through app_user_role)
               │
┌──────────────┴──────────────────────────────────────────────────┐
│                      app_user_role                              │
├─────────────────────────────────────────────────────────────────┤
│ PK │ user_id         BIGINT        NOT NULL                     │
│ PK │ role_id         BIGINT        NOT NULL                     │
│    │ created_at      TIMESTAMPTZ   NOT NULL                     │
│    │ updated_at      TIMESTAMPTZ                                │
│    │ deleted_at      TIMESTAMPTZ                                │
│    │ created_by      VARCHAR(100)  NOT NULL                     │
│    │ updated_by      VARCHAR(100)                               │
│    │ deleted_by      VARCHAR(100)                               │
│    │ is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE       │
│ FK │ user_id  ──────> app_users(id) ON DELETE CASCADE           │
│ FK │ role_id  ──────> app_roles(id) ON DELETE CASCADE           │
└──────────────┬──────────────────────────────────────────────────┘
               │
               │ N
               │
               │ 1
               │
┌──────────────┴──────────────────────────────────────────────────┐
│                         app_roles                               │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id              BIGSERIAL                                  │
│    │ name            VARCHAR(50)   UNIQUE NOT NULL              │
│    │ description     VARCHAR(255)                               │
│    │ created_at      TIMESTAMPTZ   NOT NULL                     │
│    │ updated_at      TIMESTAMPTZ                                │
│    │ deleted_at      TIMESTAMPTZ                                │
│    │ created_by      VARCHAR(100)  NOT NULL                     │
│    │ updated_by      VARCHAR(100)                               │
│    │ deleted_by      VARCHAR(100)                               │
│    │ is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE       │
└─────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                      refresh_tokens                             │
├─────────────────────────────────────────────────────────────────┤
│ PK │ id              BIGSERIAL                                  │
│ FK │ user_id         BIGINT        NOT NULL                     │
│    │ token           VARCHAR(255)  UNIQUE NOT NULL              │
│    │ issued_at       TIMESTAMPTZ   NOT NULL                     │
│    │ expires_at      TIMESTAMPTZ   NOT NULL                     │
│    │ revoked         BOOLEAN       NOT NULL DEFAULT FALSE       │
│    │ revoked_at      TIMESTAMPTZ                                │
│    │ ip_address      VARCHAR(50)                                │
│    │ user_agent      VARCHAR(500)                               │
│    │ created_at      TIMESTAMPTZ   NOT NULL                     │
│    │ updated_at      TIMESTAMPTZ                                │
│    │ deleted_at      TIMESTAMPTZ                                │
│    │ created_by      VARCHAR(100)  NOT NULL                     │
│    │ updated_by      VARCHAR(100)                               │
│    │ deleted_by      VARCHAR(100)                               │
│    │ is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE       │
│ FK │ user_id  ──────> app_users(id) ON DELETE CASCADE           │
└─────────────────────────────────────────────────────────────────┘
```

---

## Chi Tiết Các Bảng

### 1. app_users

**Mục đích**: Lưu trữ thông tin người dùng của hệ thống

**Quan hệ**:
- Many-to-Many với `app_roles` thông qua `app_user_role`
- One-to-Many với `refresh_tokens`

| Column        | Type         | Constraints                  | Description                                    |
|---------------|--------------|------------------------------|------------------------------------------------|
| id            | BIGSERIAL    | PRIMARY KEY                  | ID tự tăng, định danh duy nhất của user       |
| username      | VARCHAR(100) | UNIQUE, NOT NULL             | Tên đăng nhập, duy nhất trong hệ thống        |
| password_hash | VARCHAR(255) | NOT NULL                     | Mật khẩu đã được mã hóa bằng BCrypt            |
| full_name     | VARCHAR(255) | NULL                         | Họ tên đầy đủ của người dùng                   |
| enabled       | BOOLEAN      | NOT NULL, DEFAULT TRUE       | Trạng thái kích hoạt của tài khoản             |
| created_at    | TIMESTAMPTZ  | NOT NULL, DEFAULT NOW()      | Thời gian tạo record                           |
| updated_at    | TIMESTAMPTZ  | DEFAULT NOW()                | Thời gian cập nhật lần cuối                    |
| deleted_at    | TIMESTAMPTZ  | NULL                         | Thời gian soft delete (NULL = chưa xóa)        |
| created_by    | VARCHAR(100) | NOT NULL                     | Username của người tạo record                  |
| updated_by    | VARCHAR(100) | NULL                         | Username của người cập nhật lần cuối           |
| deleted_by    | VARCHAR(100) | NULL                         | Username của người thực hiện soft delete       |
| is_deleted    | BOOLEAN      | NOT NULL, DEFAULT FALSE      | Flag đánh dấu record đã bị xóa (soft delete)   |

**Indexes**:
```sql
-- Unique index cho username chưa bị xóa
CREATE INDEX idx_app_users_username ON app_users(username) WHERE is_deleted = FALSE;

-- Index cho soft delete queries
CREATE INDEX idx_app_users_is_deleted ON app_users(is_deleted);
```

**Sample Data**:
```sql
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES ('admin', '$2a$10$...', 'System Administrator', true, 'SYSTEM', false);

INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES ('user01', '$2a$10$...', 'Nguyen Van A', true, 'SYSTEM', false);
```

---

### 2. app_roles

**Mục đích**: Định nghĩa các vai trò (roles) trong hệ thống phân quyền

**Quan hệ**:
- Many-to-Many với `app_users` thông qua `app_user_role`

| Column      | Type         | Constraints             | Description                                    |
|-------------|--------------|-------------------------|------------------------------------------------|
| id          | BIGSERIAL    | PRIMARY KEY             | ID tự tăng, định danh duy nhất của role        |
| name        | VARCHAR(50)  | UNIQUE, NOT NULL        | Tên role (VD: USER, ADMIN, MANAGER)            |
| description | VARCHAR(255) | NULL                    | Mô tả chức năng của role                       |
| created_at  | TIMESTAMPTZ  | NOT NULL, DEFAULT NOW() | Thời gian tạo record                           |
| updated_at  | TIMESTAMPTZ  | DEFAULT NOW()           | Thời gian cập nhật lần cuối                    |
| deleted_at  | TIMESTAMPTZ  | NULL                    | Thời gian soft delete                          |
| created_by  | VARCHAR(100) | NOT NULL                | Username của người tạo record                  |
| updated_by  | VARCHAR(100) | NULL                    | Username của người cập nhật lần cuối           |
| deleted_by  | VARCHAR(100) | NULL                    | Username của người thực hiện soft delete       |
| is_deleted  | BOOLEAN      | NOT NULL, DEFAULT FALSE | Flag soft delete                               |

**Indexes**:
```sql
CREATE INDEX idx_app_roles_name ON app_roles(name) WHERE is_deleted = FALSE;
CREATE INDEX idx_app_roles_is_deleted ON app_roles(is_deleted);
```

**Sample Data**:
```sql
INSERT INTO app_roles (name, description, created_by, is_deleted)
VALUES ('USER', 'Default user role', 'SYSTEM', false);

INSERT INTO app_roles (name, description, created_by, is_deleted)
VALUES ('ADMIN', 'Administrator role', 'SYSTEM', false);

INSERT INTO app_roles (name, description, created_by, is_deleted)
VALUES ('MANAGER', 'Manager role with elevated permissions', 'SYSTEM', false);
```

---

### 3. app_user_role

**Mục đích**: Bảng trung gian (junction table) cho quan hệ Many-to-Many giữa users và roles

**Quan hệ**:
- Many-to-One với `app_users`
- Many-to-One với `app_roles`

| Column      | Type         | Constraints                      | Description                                    |
|-------------|--------------|----------------------------------|------------------------------------------------|
| user_id     | BIGINT       | PRIMARY KEY, NOT NULL, FK        | ID của user                                    |
| role_id     | BIGINT       | PRIMARY KEY, NOT NULL, FK        | ID của role                                    |
| created_at  | TIMESTAMPTZ  | NOT NULL, DEFAULT NOW()          | Thời gian gán role cho user                    |
| updated_at  | TIMESTAMPTZ  | DEFAULT NOW()                    | Thời gian cập nhật lần cuối                    |
| deleted_at  | TIMESTAMPTZ  | NULL                             | Thời gian soft delete                          |
| created_by  | VARCHAR(100) | NOT NULL                         | Username của người gán role                    |
| updated_by  | VARCHAR(100) | NULL                             | Username của người cập nhật lần cuối           |
| deleted_by  | VARCHAR(100) | NULL                             | Username của người thực hiện soft delete       |
| is_deleted  | BOOLEAN      | NOT NULL, DEFAULT FALSE          | Flag soft delete                               |

**Constraints**:
```sql
-- Composite primary key
PRIMARY KEY (user_id, role_id)

-- Foreign keys with cascade delete
CONSTRAINT fk_user_role_user FOREIGN KEY (user_id)
    REFERENCES app_users(id) ON DELETE CASCADE

CONSTRAINT fk_user_role_role FOREIGN KEY (role_id)
    REFERENCES app_roles(id) ON DELETE CASCADE
```

**Sample Data**:
```sql
-- Assign ADMIN role to admin user
INSERT INTO app_user_role (user_id, role_id, created_by, is_deleted)
VALUES (1, 2, 'SYSTEM', false);

-- Assign USER role to user01
INSERT INTO app_user_role (user_id, role_id, created_by, is_deleted)
VALUES (2, 1, 'SYSTEM', false);
```

---

### 4. refresh_tokens

**Mục đích**: Lưu trữ refresh tokens để gia hạn access tokens và tracking sessions

**Quan hệ**:
- Many-to-One với `app_users`

| Column      | Type         | Constraints                  | Description                                        |
|-------------|--------------|------------------------------|----------------------------------------------------|
| id          | BIGSERIAL    | PRIMARY KEY                  | ID tự tăng, định danh duy nhất                     |
| user_id     | BIGINT       | NOT NULL, FK                 | ID của user sở hữu token                           |
| token       | VARCHAR(255) | UNIQUE, NOT NULL             | Refresh token (UUID format)                        |
| issued_at   | TIMESTAMPTZ  | NOT NULL                     | Thời gian phát hành token                          |
| expires_at  | TIMESTAMPTZ  | NOT NULL                     | Thời gian hết hạn token                            |
| revoked     | BOOLEAN      | NOT NULL, DEFAULT FALSE      | Token đã bị thu hồi hay chưa                       |
| revoked_at  | TIMESTAMPTZ  | NULL                         | Thời gian thu hồi token                            |
| ip_address  | VARCHAR(50)  | NULL                         | IP address của client khi tạo token                |
| user_agent  | VARCHAR(500) | NULL                         | User agent (browser/device info) của client        |
| created_at  | TIMESTAMPTZ  | NOT NULL, DEFAULT NOW()      | Thời gian tạo record                               |
| updated_at  | TIMESTAMPTZ  | DEFAULT NOW()                | Thời gian cập nhật lần cuối                        |
| deleted_at  | TIMESTAMPTZ  | NULL                         | Thời gian soft delete                              |
| created_by  | VARCHAR(100) | NOT NULL                     | Username của người tạo (thường là SYSTEM)          |
| updated_by  | VARCHAR(100) | NULL                         | Username của người cập nhật lần cuối               |
| deleted_by  | VARCHAR(100) | NULL                         | Username của người thực hiện soft delete           |
| is_deleted  | BOOLEAN      | NOT NULL, DEFAULT FALSE      | Flag soft delete                                   |

**Constraints**:
```sql
CONSTRAINT fk_user_id FOREIGN KEY (user_id)
    REFERENCES app_users(id) ON DELETE CASCADE
```

**Indexes**:
```sql
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);
```

**Sample Data**:
```sql
INSERT INTO refresh_tokens (
    user_id, token, issued_at, expires_at,
    revoked, ip_address, user_agent, created_by, is_deleted
)
VALUES (
    1,
    '550e8400-e29b-41d4-a716-446655440000',
    NOW(),
    NOW() + INTERVAL '7 days',
    false,
    '192.168.1.100',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0',
    'SYSTEM',
    false
);
```

---

## Audit Fields (Shared across all tables)

Tất cả các bảng đều kế thừa các audit fields từ `BaseEntity`:

| Field       | Purpose                                                     |
|-------------|-------------------------------------------------------------|
| created_at  | Timestamp khi record được tạo                               |
| created_by  | Username của người tạo record                               |
| updated_at  | Timestamp lần cập nhật cuối cùng                            |
| updated_by  | Username của người cập nhật cuối cùng                       |
| deleted_at  | Timestamp khi record bị soft delete (NULL nếu chưa xóa)     |
| deleted_by  | Username của người thực hiện soft delete                    |
| is_deleted  | Boolean flag để query nhanh các record chưa bị xóa          |

**Benefits**:
- **Audit Trail**: Track được ai làm gì, khi nào
- **Soft Delete**: Không mất dữ liệu, có thể restore
- **Compliance**: Đáp ứng yêu cầu audit và compliance
- **Debugging**: Dễ dàng trace lỗi và investigate issues

---

## Relationships Summary

### app_users ↔ app_roles (Many-to-Many)
- Một user có thể có nhiều roles
- Một role có thể được gán cho nhiều users
- Junction table: `app_user_role`
- Cascade delete: Khi xóa user hoặc role, các mapping cũng bị xóa

### app_users ↔ refresh_tokens (One-to-Many)
- Một user có thể có nhiều refresh tokens (multi-device login)
- Mỗi refresh token thuộc về một user duy nhất
- Cascade delete: Khi xóa user, tất cả refresh tokens cũng bị xóa

---

## Data Integrity Rules

### 1. Username Uniqueness
- Username phải unique trong số các users chưa bị xóa
- Cho phép reuse username sau khi soft delete
- Index: `idx_app_users_username`

### 2. Role Name Uniqueness
- Role name phải unique trong số các roles chưa bị xóa
- Index: `idx_app_roles_name`

### 3. Refresh Token Uniqueness
- Token phải unique (UUID ensures this)
- Không có duplicate token trong hệ thống

### 4. Referential Integrity
- Foreign keys đảm bảo data consistency
- Cascade delete giữ database clean
- Soft delete cho phép restore data

### 5. Password Security
- Không bao giờ store plaintext password
- BCrypt hash với cost factor phù hợp
- Minimum password length: 8 characters

---

## Query Patterns

### Active Records Only
```sql
-- Lấy tất cả users chưa bị xóa
SELECT * FROM app_users WHERE is_deleted = false;

-- Lấy user cụ thể chưa bị xóa
SELECT * FROM app_users
WHERE username = 'user01' AND is_deleted = false;
```

### User with Roles
```sql
SELECT u.*, r.name as role_name
FROM app_users u
JOIN app_user_role ur ON u.id = ur.user_id
JOIN app_roles r ON ur.role_id = r.id
WHERE u.username = 'admin'
  AND u.is_deleted = false
  AND ur.is_deleted = false
  AND r.is_deleted = false;
```

### Active Refresh Tokens
```sql
SELECT * FROM refresh_tokens
WHERE user_id = 1
  AND is_deleted = false
  AND revoked = false
  AND expires_at > NOW();
```

### Expired Tokens Cleanup
```sql
UPDATE refresh_tokens
SET is_deleted = true,
    deleted_at = NOW(),
    deleted_by = 'SYSTEM'
WHERE expires_at < NOW()
  AND is_deleted = false;
```

---

## Performance Considerations

### Indexes Strategy
1. **Unique indexes**: username, role name, token
2. **Foreign key indexes**: user_id trong các bảng liên quan
3. **Query optimization indexes**: is_deleted, revoked
4. **Partial indexes**: Chỉ index records chưa bị xóa

### Table Partitioning (Future)
- Có thể partition `refresh_tokens` theo thời gian
- Giúp improve performance khi data lớn
- Dễ dàng archive old data

### Cleanup Strategy
- Scheduled job chạy hàng ngày
- Soft delete expired tokens
- Hard delete sau 90 ngày (optional)
