CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE incident (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    external_alert_id VARCHAR(120) NOT NULL,
    incident_type VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_incident_tenant_external_alert
        UNIQUE (tenant_id, external_alert_id)
);

CREATE INDEX ix_incident_tenant_status_received
    ON incident (tenant_id, status, received_at DESC);
