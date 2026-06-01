CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    email VARCHAR(180),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_contacts_organization_phone UNIQUE (organization_id, phone_number),
    CONSTRAINT fk_contacts_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id)
);

CREATE INDEX idx_contacts_organization_id ON contacts (organization_id);
CREATE INDEX idx_contacts_organization_name ON contacts (organization_id, name);
CREATE INDEX idx_contacts_organization_phone ON contacts (organization_id, phone_number);
