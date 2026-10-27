-- V4: Appointments
CREATE TABLE appointment (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    reference     VARCHAR(20)  NOT NULL COMMENT 'Human-readable non-guessable reference e.g. QL-20241027-A3X9',
    patient_id    BIGINT       NOT NULL COMMENT 'FK to patient_profile.id',
    doctor_id     BIGINT       NOT NULL,
    branch_id     BIGINT       NOT NULL,
    department_id BIGINT       NOT NULL,
    starts_at     DATETIME(6)  NOT NULL,
    ends_at       DATETIME(6)  NOT NULL,
    status        ENUM('CONFIRMED','CANCELLED','COMPLETED','NO_SHOW') NOT NULL DEFAULT 'CONFIRMED',
    notes         VARCHAR(1000),
    version       INT          NOT NULL DEFAULT 0 COMMENT 'Optimistic locking',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_appointment PRIMARY KEY (id),
    CONSTRAINT uk_appointment_reference UNIQUE (reference),
    -- Prevent double-booking: same doctor cannot have two confirmed appointments at the same start time
    CONSTRAINT uk_appointment_doctor_slot UNIQUE (doctor_id, starts_at),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient_profile (id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_appointment_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch (id),
    CONSTRAINT fk_appointment_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT chk_appointment_times CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appointment_patient ON appointment (patient_id, starts_at);
CREATE INDEX idx_appointment_doctor_date ON appointment (doctor_id, starts_at);
CREATE INDEX idx_appointment_status ON appointment (status, starts_at);
