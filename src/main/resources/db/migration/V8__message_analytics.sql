ALTER TABLE messages
    ADD COLUMN contact_id UUID,
    ADD COLUMN campaign_id UUID,
    ADD COLUMN failed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_contact
        FOREIGN KEY (contact_id)
        REFERENCES contacts (id);

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns (id);

UPDATE messages
SET status = 'ACCEPTED'
WHERE status = 'PENDING';

CREATE TABLE message_status_history (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    previous_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    webhook_payload TEXT NOT NULL,
    status_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_message_status_history_message
        FOREIGN KEY (message_id)
        REFERENCES messages (id),
    CONSTRAINT uk_message_status_history_transition
        UNIQUE (message_id, new_status, status_timestamp)
);

CREATE TABLE message_reports (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    report_type VARCHAR(120) NOT NULL,
    date_from TIMESTAMP WITH TIME ZONE,
    date_to TIMESTAMP WITH TIME ZONE,
    total_messages BIGINT NOT NULL,
    filters TEXT NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_message_reports_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
);

CREATE INDEX idx_messages_whatsapp_message_id ON messages (meta_message_id);
CREATE INDEX idx_messages_current_status ON messages (status);
CREATE INDEX idx_messages_template_name ON messages (template_name);
CREATE INDEX idx_messages_campaign_id ON messages (campaign_id);
CREATE INDEX idx_messages_contact_id ON messages (contact_id);
CREATE INDEX idx_messages_sent_at ON messages (sent_at);
CREATE INDEX idx_message_status_history_message_id ON message_status_history (message_id);
CREATE INDEX idx_message_status_history_new_status ON message_status_history (new_status);
CREATE INDEX idx_message_status_history_timestamp ON message_status_history (status_timestamp);
