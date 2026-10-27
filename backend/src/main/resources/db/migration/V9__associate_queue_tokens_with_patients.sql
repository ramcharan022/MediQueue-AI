ALTER TABLE queue_token
    ADD COLUMN patient_user_id BIGINT NULL AFTER queue_id;

UPDATE queue_token t
JOIN appointment a ON a.id = t.appointment_id
JOIN patient_profile p ON p.id = a.patient_id
SET t.patient_user_id = p.user_id
WHERE t.patient_user_id IS NULL;

ALTER TABLE queue_token
    ADD CONSTRAINT fk_queue_token_patient_user
        FOREIGN KEY (patient_user_id) REFERENCES user (id) ON DELETE SET NULL;

CREATE INDEX idx_queue_token_patient_user
    ON queue_token (patient_user_id, created_at);
