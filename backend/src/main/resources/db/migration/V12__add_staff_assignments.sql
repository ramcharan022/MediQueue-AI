CREATE TABLE staff_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    CONSTRAINT pk_staff_assignment PRIMARY KEY (id),
    CONSTRAINT uk_staff_assignment_user UNIQUE (user_id),
    CONSTRAINT fk_staff_assignment_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_assignment_branch FOREIGN KEY (branch_id) REFERENCES hospital_branch (id),
    CONSTRAINT fk_staff_assignment_department FOREIGN KEY (department_id) REFERENCES department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
