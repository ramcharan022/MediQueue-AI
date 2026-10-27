-- V5: Queues, tokens, and transition events
CREATE TABLE queue (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    service_date  DATE   NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_queue PRIMARY KEY (id),
    CONSTRAINT uk_queue_scope UNIQUE (branch_id, department_id, service_date),
    CONSTRAINT fk_queue_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch (id),
    CONSTRAINT fk_queue_department FOREIGN KEY (department_id) REFERENCES department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE queue_token (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    queue_id       BIGINT      NOT NULL,
    appointment_id BIGINT,
    doctor_id      BIGINT,
    token_number   VARCHAR(10) NOT NULL COMMENT 'e.g. A001, B023',
    status         ENUM('WAITING','CALLED','IN_SERVICE','COMPLETED','SKIPPED','NO_SHOW','CANCELLED') NOT NULL DEFAULT 'WAITING',
    allocation_reason VARCHAR(500) COMMENT 'Explainable allocation decision',
    estimated_wait_minutes INT,
    version        INT         NOT NULL DEFAULT 0 COMMENT 'Optimistic locking version',
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_queue_token PRIMARY KEY (id),
    CONSTRAINT uk_queue_token_number UNIQUE (queue_id, token_number),
    CONSTRAINT fk_queue_token_queue FOREIGN KEY (queue_id) REFERENCES queue (id),
    CONSTRAINT fk_queue_token_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id),
    CONSTRAINT fk_queue_token_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_queue_token_status ON queue_token (queue_id, status, id);

CREATE TABLE queue_event (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    token_id        BIGINT      NOT NULL,
    actor_user_id   BIGINT      COMMENT 'NULL for system-initiated transitions',
    previous_status VARCHAR(20) NOT NULL,
    next_status     VARCHAR(20) NOT NULL,
    reason          VARCHAR(500),
    occurred_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_queue_event PRIMARY KEY (id),
    CONSTRAINT fk_queue_event_token FOREIGN KEY (token_id) REFERENCES queue_token (id),
    CONSTRAINT fk_queue_event_actor FOREIGN KEY (actor_user_id) REFERENCES user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_queue_event_token ON queue_event (token_id, occurred_at);
