# Authentication Service - Mô Tả Chức Năng

## Tổng Quan

**Authentication Service** là một microservice độc lập chịu trách nhiệm xử lý toàn bộ luồng xác thực (authentication) và phân quyền (authorization) cho hệ thống HDBank. Service sử dụng JWT (JSON Web Token) để quản lý phiên làm việc và Refresh Token để gia hạn access token.

## Các Chức Năng Chính

### 1. Đăng Ký Người Dùng (User Registration)

**Mục đích**: Cho phép người dùng mới tạo tài khoản trong hệ thống.

**Endpoint**: `POST /api/v1/auth/register`

**Tính năng**:
- Tạo tài khoản người dùng mới với username và password
- Mã hóa password bằng BCrypt
- Tự động gán role mặc định "USER" cho người dùng mới
- Kiểm tra tính duy nhất của username
- Validation đầu vào:
  - Username: 3-100 ký tự
  - Password: tối thiểu 8 ký tự

**Dữ liệu trả về**:
- Thông tin người dùng (UserInfo): id, username, fullName, enabled, roles

---

### 2. Đăng Nhập (User Login)

**Mục đích**: Xác thực thông tin đăng nhập và cấp phát JWT tokens.

**Endpoint**: `POST /api/v1/auth/login`

**Tính năng**:
- Xác thực username và password
- Sinh access token (JWT) có thời gian hiệu lực ngắn (mặc định: 15 phút)
- Sinh refresh token có thời gian hiệu lực dài (mặc định: 7 ngày)
- Lưu trữ metadata của refresh token:
  - IP address của client
  - User agent (thông tin trình duyệt/thiết bị)
  - Thời gian phát hành và hết hạn
- Soft delete các refresh token cũ của người dùng khi đăng nhập mới

**Dữ liệu trả về**:
- Access token (JWT)
- Refresh token (UUID)
- Token type: "Bearer"
- Expires in: thời gian sống của access token (giây)
- Thông tin người dùng (UserInfo)

---

### 3. Làm Mới Access Token (Refresh Token)

**Mục đích**: Cấp phát access token mới mà không cần đăng nhập lại.

**Endpoint**: `POST /api/v1/auth/refresh`

**Tính năng**:
- Xác thực refresh token
- Kiểm tra refresh token còn hiệu lực (chưa hết hạn, chưa bị revoke)
- Thu hồi refresh token cũ
- Sinh cặp token mới (access token + refresh token)
- Cập nhật IP address và user agent mới

**Dữ liệu trả về**:
- Access token mới (JWT)
- Refresh token mới (UUID)
- Token type: "Bearer"
- Expires in: thời gian sống của access token
- Thông tin người dùng (UserInfo)

---

### 4. Đăng Xuất (Logout)

**Mục đích**: Thu hồi refresh token để vô hiệu hóa phiên làm việc.

**Endpoint**: `POST /api/v1/auth/logout`

**Yêu cầu**: Access token hợp lệ trong header

**Tính năng**:
- Thu hồi (revoke) refresh token được cung cấp
- Đánh dấu refresh token là đã bị revoke
- Ghi nhận thời gian revoke

**Dữ liệu trả về**:
- Thông báo thành công

---

### 5. Lấy Thông Tin Người Dùng Hiện Tại (Get Current User)

**Mục đích**: Lấy thông tin chi tiết của người dùng đang đăng nhập.

**Endpoint**: `GET /api/v1/auth/me`

**Yêu cầu**: Access token hợp lệ trong header

**Tính năng**:
- Trích xuất thông tin người dùng từ JWT token
- Trả về thông tin chi tiết của người dùng
- Bao gồm danh sách các role được gán

**Dữ liệu trả về**:
- UserInfo: id, username, fullName, enabled, roles

---

### 6. Đổi Mật Khẩu (Change Password)

**Mục đích**: Cho phép người dùng thay đổi mật khẩu của mình.

**Endpoint**: `PUT /api/v1/auth/change-password`

**Yêu cầu**: Access token hợp lệ trong header

**Tính năng**:
- Xác thực mật khẩu cũ
- Kiểm tra độ mạnh của mật khẩu mới (tối thiểu 8 ký tự)
- Mã hóa và lưu mật khẩu mới
- **Bảo mật**: Tự động thu hồi TẤT CẢ refresh tokens của người dùng
  - Buộc người dùng đăng nhập lại trên tất cả thiết bị
  - Ngăn chặn việc sử dụng session cũ sau khi đổi mật khẩu

**Dữ liệu trả về**:
- Thông báo thành công

---

## Các Tính Năng Bảo Mật

### 1. JWT Authentication
- Access token được ký bằng thuật toán HS256
- Chứa thông tin: username, roles, thời gian phát hành, thời gian hết hạn
- Token ngắn hạn để giảm thiểu rủi ro nếu bị lộ

### 2. Refresh Token Management
- Refresh token dạng UUID ngẫu nhiên
- Lưu trữ trong database để có thể thu hồi
- Tracking metadata (IP, User Agent) để phát hiện hoạt động bất thường
- Soft delete các token cũ khi đăng nhập mới

### 3. Password Security
- Mã hóa password bằng BCrypt (cost factor mặc định)
- Không lưu trữ plaintext password
- Validation độ dài và độ phức tạp

### 4. Audit Trail
- Tất cả entities kế thừa từ BaseEntity với audit fields:
  - created_by, created_at
  - updated_by, updated_at
  - deleted_by, deleted_at
- Soft delete cho tất cả records
- Tracking tất cả thay đổi trong database

### 5. Scheduled Tasks
- Tự động dọn dẹp refresh token hết hạn theo lịch định kỳ
- Giảm thiểu dữ liệu rác trong database

### 6. API Security
- Protected endpoints yêu cầu JWT token hợp lệ
- Global exception handling cho tất cả lỗi
- Return codes chuẩn:
  - 200: Success
  - 400: Bad Request (validation error)
  - 401: Unauthorized (missing/invalid token)
  - 404: Not Found (resource không tồn tại)

---

## Use Cases

### Use Case 1: Đăng Ký và Đăng Nhập Lần Đầu
1. Người dùng đăng ký tài khoản mới
2. Hệ thống tạo user với role USER
3. Người dùng đăng nhập
4. Nhận access token và refresh token
5. Sử dụng access token để truy cập protected APIs

### Use Case 2: Session Gia Hạn Tự Động
1. Access token hết hạn sau 1 giờ
2. Client tự động gọi /refresh với refresh token
3. Nhận access token mới và refresh token mới
4. Tiếp tục sử dụng mà không cần đăng nhập lại

### Use Case 3: Đổi Mật Khẩu và Bảo Mật
1. Người dùng phát hiện tài khoản có khả năng bị xâm nhập
2. Đổi mật khẩu thông qua /change-password
3. Hệ thống tự động revoke tất cả refresh tokens
4. Tất cả thiết bị khác bị đăng xuất
5. Người dùng phải đăng nhập lại ở mọi nơi

### Use Case 4: Multi-Device Login
1. Người dùng đăng nhập trên điện thoại
2. Người dùng đăng nhập trên laptop
3. Refresh token cũ trên điện thoại bị soft delete
4. Chỉ session mới nhất (laptop) còn active refresh token
5. Điện thoại cần đăng nhập lại

---

## Kiến Trúc Kỹ Thuật

### Technology Stack
- **Framework**: Spring Boot 3.5.9
- **Java**: 21
- **Security**: Spring Security + JWT (jjwt 0.12.3)
- **Database**: PostgreSQL (production), H2 (testing)
- **ORM**: Spring Data JPA
- **Migration**: Flyway
- **Documentation**: SpringDoc OpenAPI 3.0

### Design Patterns
- **Layered Architecture**: Controller → Service → Repository
- **DTO Pattern**: Separation of API models and entities
- **Repository Pattern**: Data access abstraction
- **Dependency Injection**: Constructor injection with Lombok
- **Builder Pattern**: Entity và DTO construction

### Best Practices
- Transactional boundaries tại service layer
- Read-only transactions cho queries
- Soft delete instead of hard delete
- Comprehensive exception handling
- Logging cho security events
- Integration tests coverage
