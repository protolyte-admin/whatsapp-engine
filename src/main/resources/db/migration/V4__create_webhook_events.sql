ALTER TABLE messages
    ADD COLUMN direction VARCHAR(40) NOT NULL DEFAULT 'OUTBOUND',
    ADD COLUMN raw_webhook_payload TEXT;

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    organization_id UUID,
    provider VARCHAR(80) NOT NULL,
    phone_number_id VARCHAR(80),
    meta_message_id VARCHAR(120),
    event_type VARCHAR(80),
    status VARCHAR(40) NOT NULL,
    raw_payload TEXT NOT NULL,
    error_message TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_webhook_events_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
);

CREATE INDEX idx_webhook_events_organization_id ON webhook_events (organization_id);
CREATE INDEX idx_webhook_events_meta_message_id ON webhook_events (meta_message_id);
CREATE INDEX idx_webhook_events_status ON webhook_events (status);
