ALTER TABLE organizations
    ADD COLUMN whatsapp_business_account_id VARCHAR(80),
    ADD COLUMN whatsapp_phone_number_id VARCHAR(80),
    ADD COLUMN whatsapp_access_token TEXT;

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    created_by_user_id UUID,
    recipient_phone_number VARCHAR(32) NOT NULL,
    message_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    meta_message_id VARCHAR(120),
    text_body TEXT,
    template_name VARCHAR(150),
    template_language VARCHAR(20),
    template_parameters TEXT,
    failure_reason TEXT,
    meta_response TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_messages_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id),
    CONSTRAINT fk_messages_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (id)
);

CREATE INDEX idx_messages_organization_id ON messages (organization_id);
CREATE INDEX idx_messages_meta_message_id ON messages (meta_message_id);
CREATE INDEX idx_messages_status ON messages (status);
