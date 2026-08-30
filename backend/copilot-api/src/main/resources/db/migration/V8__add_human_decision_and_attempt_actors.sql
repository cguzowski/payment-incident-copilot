ALTER TABLE evidence_collection_attempt
    ADD COLUMN requested_by UUID;

ALTER TABLE knowledge_retrieval_attempt
    ADD COLUMN requested_by UUID;

ALTER TABLE report_generation_attempt
    ADD CONSTRAINT uk_report_attempt_decision_reference
        UNIQUE (
            tenant_id,
            id,
            investigation_id,
            incident_id,
            investigation_correlation_id
        );

CREATE TABLE human_decision (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    investigation_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    investigation_correlation_id UUID NOT NULL,
    report_attempt_id UUID NOT NULL,
    decided_by UUID NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_human_decision_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_human_decision_investigation UNIQUE (tenant_id, investigation_id),
    CONSTRAINT uk_human_decision_report UNIQUE (tenant_id, report_attempt_id),
    CONSTRAINT fk_human_decision_investigation
        FOREIGN KEY (tenant_id, investigation_id, investigation_correlation_id)
        REFERENCES investigation (tenant_id, id, correlation_id),
    CONSTRAINT fk_human_decision_incident
        FOREIGN KEY (tenant_id, incident_id)
        REFERENCES incident (tenant_id, id),
    CONSTRAINT fk_human_decision_report
        FOREIGN KEY (
            tenant_id,
            report_attempt_id,
            investigation_id,
            incident_id,
            investigation_correlation_id
        ) REFERENCES report_generation_attempt (
            tenant_id,
            id,
            investigation_id,
            incident_id,
            investigation_correlation_id
        ),
    CONSTRAINT ck_human_decision_outcome
        CHECK (outcome IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_human_decision_reason
        CHECK (
            reason = BTRIM(reason)
            AND BTRIM(reason) <> ''
            AND CHAR_LENGTH(reason) <= 1000
        )
);

CREATE INDEX ix_human_decision_tenant_decided
    ON human_decision (tenant_id, decided_at DESC, id DESC);
