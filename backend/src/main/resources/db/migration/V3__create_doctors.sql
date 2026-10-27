-- V3: Doctors and their weekly schedules
CREATE TABLE doctor (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id            BIGINT       NOT NULL,
    department_id        BIGINT       NOT NULL,
    user_id              BIGINT,
    full_name            VARCHAR(120) NOT NULL,
    specialization       VARCHAR(120) NOT NULL,
    qualification        VARCHAR(255),
    consultation_minutes INT          NOT NULL DEFAULT 15,
    is_active            TINYINT(1)   NOT NULL DEFAULT 1,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_doctor PRIMARY KEY (id),
    CONSTRAINT fk_doctor_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch (id),
    CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- day_of_week: 1=Monday ... 7=Sunday (ISO-8601)
CREATE TABLE doctor_schedule (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    doctor_id   BIGINT    NOT NULL,
    day_of_week TINYINT   NOT NULL COMMENT '1=Mon,7=Sun',
    starts_at   TIME      NOT NULL,
    ends_at     TIME      NOT NULL,
    CONSTRAINT pk_doctor_schedule PRIMARY KEY (id),
    CONSTRAINT fk_doctor_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id) ON DELETE CASCADE,
    CONSTRAINT chk_doctor_schedule_times CHECK (ends_at > starts_at),
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
