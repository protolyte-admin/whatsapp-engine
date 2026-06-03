ALTER TABLE message_status_history
    ADD COLUMN to_phone_number VARCHAR(32);

UPDATE message_status_history history
SET to_phone_number = message.recipient_phone_number
FROM messages message
WHERE history.message_id = message.id
  AND history.to_phone_number IS NULL;

CREATE INDEX idx_message_status_history_to_phone_number ON message_status_history (to_phone_number);
