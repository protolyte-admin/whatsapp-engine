CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    message_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    text_body TEXT,
    template_name VARCHAR(150),
    template_language VARCHAR(20),
    template_parameters TEXT,
    scheduled_at TIMESTAMP WITH TIME ZONE,
    launched_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    total_recipients INTEGER NOT NULL DEFAULT 0,
    sent_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_campaigns_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
);

CREATE TABLE campaign_messages (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    contact_id UUID NOT NULL,
    message_id UUID,
    status VARCHAR(40) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    failure_reason TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_campaign_messages_campaign
        FOREIGN KEY (campaign_id)
        REFERENCES campaigns (id),
    CONSTRAINT fk_campaign_messages_contact
        FOREIGN KEY (contact_id)
        REFERENCES contacts (id),
    CONSTRAINT fk_campaign_messages_message
        FOREIGN KEY (message_id)
        REFERENCES messages (id),
    CONSTRAINT uk_campaign_messages_campaign_contact UNIQUE (campaign_id, contact_id)
);

CREATE INDEX idx_campaigns_organization_status ON campaigns (organization_id, status);
CREATE INDEX idx_campaigns_scheduled_at ON campaigns (scheduled_at);
CREATE INDEX idx_campaign_messages_campaign_id ON campaign_messages (campaign_id);
CREATE INDEX idx_campaign_messages_status ON campaign_messages (status);
