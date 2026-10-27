-- V6: Notifications, audit log, waiting list
CREATE TABLE notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(60)  NOT NULL COMMENT 'BOOKING_CONFIRMED, TOKEN_CALLED, TOKEN_NEXT, APPOINTMENT_CANCELLED, etc.',
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(1000),
    reference  VARCHAR(40)  COMMENT 'Linked entity reference (appointment ref, token id, etc.)',
    read_at    DATETIME(6),
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_notification PRIMARY KEY (id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_user_unread ON notification (user_id, read_at, created_at);

CREATE TABLE audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT       COMMENT 'NULL for system actions',
    action        VARCHAR(80)  NOT NULL COMMENT 'e.g. QUEUE_TOKEN_SKIP, APPOINTMENT_FORCE_CANCEL, DOCTOR_CREATED',
    target_type   VARCHAR(60)  COMMENT 'e.g. queue_token, appointment, doctor',
    target_id     BIGINT,
    detail        VARCHAR(1000) COMMENT 'JSON or plain-text reason/metadata',
    occurred_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_log_occurred ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id);

CREATE TABLE waiting_list_entry (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    patient_id    BIGINT      NOT NULL COMMENT 'FK to patient_profile.id',
    department_id BIGINT      NOT NULL,
    doctor_id     BIGINT      COMMENT 'NULL = any eligible doctor',
    status        ENUM('PENDING','FULFILLED','CANCELLED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    requested_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fulfilled_at  DATETIME(6),
    CONSTRAINT pk_waiting_list_entry PRIMARY KEY (id),
    CONSTRAINT fk_wl_patient FOREIGN KEY (patient_id) REFERENCES patient_profile (id),
    CONSTRAINT fk_wl_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_wl_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
