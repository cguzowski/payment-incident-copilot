ALTER TABLE investigation
    ADD CONSTRAINT uk_investigation_tenant_id_correlation
        UNIQUE (tenant_id, id, correlation_id);

CREATE TABLE evidence_collection_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    investigation_id UUID NOT NULL,
    tool_call_id UUID NOT NULL,
    investigation_correlation_id UUID NOT NULL,
    source_system VARCHAR(120) NOT NULL,
    source_tool VARCHAR(120) NOT NULL,
    scenario_reference VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    retrieved_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    content_schema_version VARCHAR(80) NOT NULL,
    content JSONB,
    status_detail VARCHAR(500),
    CONSTRAINT uk_evidence_collection_tool_call
        UNIQUE (tool_call_id),
    CONSTRAINT fk_evidence_collection_investigation
        FOREIGN KEY (tenant_id, investigation_id, investigation_correlation_id)
        REFERENCES investigation (tenant_id, id, correlation_id),
    CONSTRAINT ck_evidence_collection_status
        CHECK (status IN (
            'STARTED',
            'AVAILABLE',
            'PARTIAL',
            'NOT_FOUND',
            'UNAVAILABLE',
            'TIMED_OUT',
            'MALFORMED'
        )),
    CONSTRAINT ck_evidence_collection_completion
        CHECK (
            (status = 'STARTED' AND completed_at IS NULL)
            OR (status <> 'STARTED' AND completed_at IS NOT NULL)
        ),
    CONSTRAINT ck_evidence_collection_content
        CHECK (
            (status IN ('AVAILABLE', 'PARTIAL') AND content IS NOT NULL)
            OR (status NOT IN ('AVAILABLE', 'PARTIAL') AND content IS NULL)
        ),
    CONSTRAINT ck_evidence_collection_source_system
        CHECK (BTRIM(source_system) <> ''),
    CONSTRAINT ck_evidence_collection_source_tool
        CHECK (BTRIM(source_tool) <> ''),
    CONSTRAINT ck_evidence_collection_scenario_reference
        CHECK (BTRIM(scenario_reference) <> ''),
    CONSTRAINT ck_evidence_collection_schema_version
        CHECK (BTRIM(content_schema_version) <> '')
);

CREATE INDEX ix_evidence_collection_tenant_investigation_requested
    ON evidence_collection_attempt (
        tenant_id,
        investigation_id,
        requested_at DESC,
        id DESC
    );
