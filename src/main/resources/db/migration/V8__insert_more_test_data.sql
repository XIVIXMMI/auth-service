
-- Manager role (in addition to USER and ADMIN from V5)
INSERT INTO app_roles (name, description, created_by, is_deleted)
VALUES ('MANAGER', 'Manager role with elevated permissions', 'SYSTEM', false)
ON CONFLICT (name) DO NOTHING;

-- Admin User
-- all password is "Password123"
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'admin@hdbank.com',
    '$2a$12$3bnNxirrKtn.nxOHDE0xVOu2YimVOg1B1i1YkRS4eUXVX.bGlBALG',
    'System Administrator',
    true,
    'SYSTEM',
    false
)
ON CONFLICT (username) DO NOTHING;

-- Regular User 1
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'user01@hdbank.com',
    '$2a$12$3bnNxirrKtn.nxOHDE0xVOu2YimVOg1B1i1YkRS4eUXVX.bGlBALG',
    'Nguyen Van A',
    true,
    'SYSTEM',
    false
)
ON CONFLICT (username) DO NOTHING;

-- Regular User 2
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'user02@hdbank.com',
    '$2a$12$3bnNxirrKtn.nxOHDE0xVOu2YimVOg1B1i1YkRS4eUXVX.bGlBALG',
    'Tran Thi B',
    true,
    'SYSTEM',
    false
)
ON CONFLICT (username) DO NOTHING;

-- Manager User
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'manager@hdbank.com',
    '$2a$12$3bnNxirrKtn.nxOHDE0xVOu2YimVOg1B1i1YkRS4eUXVX.bGlBALG',
    'Le Van C',
    true,
    'SYSTEM',
    false
)
ON CONFLICT (username) DO NOTHING;

-- Disabled User (for testing disabled account scenario)
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'disabled@hdbank.com',
    '$2a$12$3bnNxirrKtn.nxOHDE0xVOu2YimVOg1B1i1YkRS4eUXVX.bGlBALG',
    'Disabled User',
    false,
    'SYSTEM',
    false
)
ON CONFLICT (username) DO NOTHING;

-- Test User for Integration Tests
INSERT INTO app_users (username, password_hash, full_name, enabled, created_by, is_deleted)
VALUES (
    'testuser@hdbank.com',
    '$2a$12$3bnNxirrKtn.nxOHDE0xVOu2YimVOg1B1i1YkRS4eUXVX.bGlBALG',
    'Test User',
    true,
    'SYSTEM',
    false
)
ON CONFLICT (username) DO NOTHING;

-- Admin user gets both ADMIN and USER roles
INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username = 'admin@hdbank.com'
  AND r.name IN ('ADMIN', 'USER')
  AND u.is_deleted = false
  AND r.is_deleted = false
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Regular users get USER role only
INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username IN ('user01@hdbank.com', 'user02@hdbank.com', 'testuser@hdbank.com')
  AND r.name = 'USER'
  AND u.is_deleted = false
  AND r.is_deleted = false
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Manager gets both MANAGER and USER roles
INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username = 'manager@hdbank.com'
  AND r.name IN ('MANAGER', 'USER')
  AND u.is_deleted = false
  AND r.is_deleted = false
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Disabled user gets USER role (but account is disabled)
INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
CROSS JOIN app_roles r
WHERE u.username = 'disabled@hdbank.com'
  AND r.name = 'USER'
  AND u.is_deleted = false
  AND r.is_deleted = false
ON CONFLICT (user_id, role_id) DO NOTHING;


-- Active refresh token for user01
-- Token: 550e8400-e29b-41d4-a716-446655440001
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
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'user01@hdbank.com'
  AND u.is_deleted = false
ON CONFLICT (token) DO NOTHING;

-- Revoked refresh token for user01 (for testing revoked token rejection)
-- Token: 550e8400-e29b-41d4-a716-446655440002
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
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'user01@hdbank.com'
  AND u.is_deleted = false
ON CONFLICT (token) DO NOTHING;

-- Expired refresh token for user02 (for testing expired token rejection)
-- Token: 550e8400-e29b-41d4-a716-446655440003
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
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'user02@hdbank.com'
  AND u.is_deleted = false
ON CONFLICT (token) DO NOTHING;

-- Active refresh token for admin (multiple devices simulation)
-- Token 1: Desktop
INSERT INTO refresh_tokens (
    user_id, token, issued_at, expires_at,
    revoked, ip_address, user_agent, created_by, is_deleted
)
SELECT
    u.id,
    '650e8400-e29b-41d4-a716-446655440001',
    NOW() - INTERVAL '30 minutes',
    NOW() + INTERVAL '6 days 23.5 hours',
    false,
    '192.168.1.50',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'admin@hdbank.com'
  AND u.is_deleted = false
ON CONFLICT (token) DO NOTHING;

-- Token 2: Mobile
INSERT INTO refresh_tokens (
    user_id, token, issued_at, expires_at,
    revoked, ip_address, user_agent, created_by, is_deleted
)
SELECT
    u.id,
    '650e8400-e29b-41d4-a716-446655440002',
    NOW() - INTERVAL '10 minutes',
    NOW() + INTERVAL '6 days 23.83 hours',
    false,
    '192.168.1.50',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
    'SYSTEM',
    false
FROM app_users u
WHERE u.username = 'admin@hdbank.com'
  AND u.is_deleted = false
ON CONFLICT (token) DO NOTHING;

-- SUMMARY OF TEST DATA
-- Users Created:
--   1. admin@hdbank.com - ADMIN + USER roles (enabled)
--   2. user01@hdbank.com - USER role (enabled)
--   3. user02@hdbank.com - USER role (enabled)
--   4. manager@hdbank.com - MANAGER + USER roles (enabled)
--   5. disabled@hdbank.com - USER role (DISABLED)
--   6. testuser@hdbank.com - USER role (enabled)
--
-- All users have password: Password@123
--
-- Refresh Tokens:
--   - user01: 1 active, 1 revoked
--   - user02: 1 expired
--   - admin: 2 active (multi-device)
