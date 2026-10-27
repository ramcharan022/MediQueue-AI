-- V2: Hospital branches and departments
CREATE TABLE hospital_branch (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(120) NOT NULL,
    address    VARCHAR(255),
    city       VARCHAR(80),
    timezone   VARCHAR(60)  NOT NULL DEFAULT 'Asia/Kolkata',
    phone      VARCHAR(30),
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_hospital_branch PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE department (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id   BIGINT       NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    is_active   TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_department PRIMARY KEY (id),
    CONSTRAINT fk_department_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
