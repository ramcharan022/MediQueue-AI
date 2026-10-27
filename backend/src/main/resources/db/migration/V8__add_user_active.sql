-- V8: Add is_active to user table for staff management
ALTER TABLE user ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 AFTER role;
