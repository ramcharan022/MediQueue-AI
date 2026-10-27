ALTER TABLE waiting_list_entry
    ADD COLUMN active_request_key VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'PENDING'
                THEN CONCAT(patient_id, ':', department_id, ':', COALESCE(doctor_id, 0))
                ELSE NULL
            END
        ) STORED;

CREATE UNIQUE INDEX uk_waiting_list_active_request
    ON waiting_list_entry (active_request_key);
