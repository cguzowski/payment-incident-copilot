ALTER TABLE incident
    ADD CONSTRAINT uk_incident_tenant_id UNIQUE (tenant_id, id);

CREATE TABLE investigation (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    started_by UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    correlation_id UUID NOT NULL,
    CONSTRAINT uk_investigation_tenant_incident
        UNIQUE (tenant_id, incident_id),
    CONSTRAINT uk_investigation_correlation
        UNIQUE (correlation_id),
    CONSTRAINT fk_investigation_tenant_incident
        FOREIGN KEY (tenant_id, incident_id)
        REFERENCES incident (tenant_id, id)
);

CREATE INDEX ix_investigation_tenant_started
    ON investigation (tenant_id, started_at DESC);
