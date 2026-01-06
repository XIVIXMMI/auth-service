INSERT INTO app_roles (name, description, created_by, created_at, is_deleted)
VALUES ('USER', 'Default user role', 'SYSTEM', NOW(), false);

INSERT INTO app_roles (name, description, created_by, created_at, is_deleted)
VALUES ('ADMIN', 'Administrator role', 'SYSTEM', NOW(), false);