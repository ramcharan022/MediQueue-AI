ALTER TABLE queue
    ADD COLUMN last_token_sequence INT NOT NULL DEFAULT 0;

UPDATE queue q
SET last_token_sequence = COALESCE((
    SELECT MAX(CAST(SUBSTRING(t.token_number, 2) AS UNSIGNED))
    FROM queue_token t
    WHERE t.queue_id = q.id
), 0);
