ALTER TABLE appointment
    MODIFY COLUMN status ENUM('CONFIRMED', 'CHECKED_IN', 'CANCELLED', 'COMPLETED', 'NO_SHOW')
        NOT NULL DEFAULT 'CONFIRMED',
    DROP INDEX uk_appointment_doctor_slot,
    ADD COLUMN active_slot_starts_at DATETIME(6)
        GENERATED ALWAYS AS (CASE WHEN status = 'CANCELLED' THEN NULL ELSE starts_at END) STORED,
    ADD CONSTRAINT uk_appointment_active_doctor_slot UNIQUE (doctor_id, active_slot_starts_at);

ALTER TABLE queue_token
    ADD COLUMN started_at DATETIME(6) NULL,
    ADD COLUMN completed_at DATETIME(6) NULL,
    ADD COLUMN active_appointment_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'CANCELLED' THEN NULL ELSE appointment_id END) STORED,
    ADD CONSTRAINT uk_queue_token_active_appointment UNIQUE (active_appointment_id);

CREATE INDEX idx_queue_token_doctor_completion
    ON queue_token (doctor_id, status, completed_at);
