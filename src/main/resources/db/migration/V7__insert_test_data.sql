INSERT INTO app_users (username, password_hash, full_name, enabled, created_at, created_by, is_deleted)
VALUES
    -- all password use "Password123"

    ('admin', '$2a$12$b/3Ep/5gYmhS17S8wmcMfuEl9mk5fANRzDXAO/DaQ2DX9xt2jIeD.',
    'System Administrator', true, NOW(), 'SYSTEM', false),

    ('test.user', '$2a$12$b/3Ep/5gYmhS17S8wmcMfuEl9mk5fANRzDXAO/DaQ2DX9xt2jIeD.',
    'Test User', true, NOW(), 'SYSTEM', false),

    ('john.doe', '$2a$12$b/3Ep/5gYmhS17S8wmcMfuEl9mk5fANRzDXAO/DaQ2DX9xt2jIeD.',
    'John Doe', true, NOW(), 'SYSTEM', false),

    ('jane.smith', '$2a$12$b/3Ep/5gYmhS17S8wmcMfuEl9mk5fANRzDXAO/DaQ2DX9xt2jIeD.',
    'Jane Smith', true, NOW(), 'SYSTEM', false),

    ('disabled.user', '$2a$12$b/3Ep/5gYmhS17S8wmcMfuEl9mk5fANRzDXAO/DaQ2DX9xt2jIeD.',
    'Disabled User', false, NOW(), 'SYSTEM', false);

INSERT INTO app_user_role (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
CROSS JOIN app_roles r
WHERE (u.username = 'admin' AND r.name IN ('USER', 'ADMIN'))
    OR (u.username IN ('test.user', 'john.doe', 'jane.smith', 'disabled.user') AND r.name = 'USER');

-- SELECT 'Users created:' as info, COUNT(*) as count FROM app_users WHERE is_deleted = false;
-- SELECT u.username, u.full_name, u.enabled, STRING_AGG(r.name, ', ' ORDER BY r.name) as roles
-- FROM app_users u
-- LEFT JOIN app_user_role ur ON u.id = ur.user_id
-- LEFT JOIN app_roles r ON ur.role_id = r.id AND r.is_deleted = false
-- WHERE u.is_deleted = false
-- GROUP BY u.id, u.username, u.full_name, u.enabled
-- ORDER BY u.username;