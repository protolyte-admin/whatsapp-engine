CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(120),
    method VARCHAR(20),
    path VARCHAR(500) NOT NULL,
    action VARCHAR(80),
    username VARCHAR(180),
    user_id UUID,
    organization_id UUID,
    client_ip VARCHAR(80),
    user_agent VARCHAR(300),
    status_code INTEGER,
    duration_ms BIGINT,
    occurred_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_logs_correlation_id ON audit_logs (correlation_id);
CREATE INDEX idx_audit_logs_organization_id ON audit_logs (organization_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs (occurred_at);
