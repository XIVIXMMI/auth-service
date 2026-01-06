ALTER TABLE app_user_role
        DROP COLUMN created_by,
        DROP COLUMN updated_by,
        DROP COLUMN deleted_by,
        DROP COLUMN created_at,
        DROP COLUMN updated_at,
        DROP COLUMN deleted_at,
        DROP COLUMN is_deleted;