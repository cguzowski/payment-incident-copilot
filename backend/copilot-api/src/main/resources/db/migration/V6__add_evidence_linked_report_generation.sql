ALTER TABLE evidence_collection_attempt
    ADD CONSTRAINT uk_evidence_collection_tenant_id UNIQUE (tenant_id, id);

ALTER TABLE knowledge_retrieval_result
    ADD CONSTRAINT uk_knowledge_retrieval_result_tenant
        UNIQUE (tenant_id, retrieval_id, chunk_id);

CREATE TABLE report_generation_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    investigation_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    investigation_correlation_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    model_id VARCHAR(200) NOT NULL,
    temperature INTEGER NOT NULL,
    max_output_tokens INTEGER NOT NULL,
    prompt_version VARCHAR(80) NOT NULL,
    prompt_hash CHAR(64) NOT NULL,
    schema_version VARCHAR(80) NOT NULL,
    schema_hash CHAR(64) NOT NULL,
    latest_evidence_id UUID NOT NULL,
    applicable_evidence_id UUID,
    retrieval_id UUID NOT NULL,
    provider_request_id VARCHAR(160),
    status_detail VARCHAR(500),
    disposition VARCHAR(30),
    report_content JSONB,
    CONSTRAINT uk_report_attempt_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT uk_report_attempt_tenant_retrieval UNIQUE (tenant_id, id, retrieval_id),
    CONSTRAINT fk_report_attempt_investigation
        FOREIGN KEY (tenant_id, investigation_id, investigation_correlation_id)
        REFERENCES investigation (tenant_id, id, correlation_id),
    CONSTRAINT fk_report_attempt_incident
        FOREIGN KEY (tenant_id, incident_id)
        REFERENCES incident (tenant_id, id),
    CONSTRAINT fk_report_attempt_latest_evidence
        FOREIGN KEY (tenant_id, latest_evidence_id)
        REFERENCES evidence_collection_attempt (tenant_id, id),
    CONSTRAINT fk_report_attempt_applicable_evidence
        FOREIGN KEY (tenant_id, applicable_evidence_id)
        REFERENCES evidence_collection_attempt (tenant_id, id),
    CONSTRAINT fk_report_attempt_retrieval
        FOREIGN KEY (tenant_id, retrieval_id)
        REFERENCES knowledge_retrieval_attempt (tenant_id, id),
    CONSTRAINT ck_report_attempt_status
        CHECK (status IN ('STARTED', 'AVAILABLE', 'UNAVAILABLE', 'TIMED_OUT', 'MALFORMED')),
    CONSTRAINT ck_report_attempt_completion
        CHECK (
            (status = 'STARTED' AND completed_at IS NULL)
            OR (status <> 'STARTED' AND completed_at IS NOT NULL)
        ),
    CONSTRAINT ck_report_attempt_content
        CHECK (
            (status = 'AVAILABLE' AND report_content IS NOT NULL AND disposition IS NOT NULL)
            OR (status <> 'AVAILABLE' AND report_content IS NULL AND disposition IS NULL)
        ),
    CONSTRAINT ck_report_attempt_disposition
        CHECK (disposition IS NULL OR disposition IN ('PROPOSED', 'INSUFFICIENT_EVIDENCE')),
    CONSTRAINT ck_report_attempt_settings
        CHECK (temperature = 0 AND max_output_tokens > 0 AND max_output_tokens <= 8192),
    CONSTRAINT ck_report_attempt_metadata
        CHECK (
            BTRIM(model_id) <> ''
            AND BTRIM(prompt_version) <> ''
            AND BTRIM(schema_version) <> ''
            AND prompt_hash ~ '^[0-9a-f]{64}$'
            AND schema_hash ~ '^[0-9a-f]{64}$'
        )
);

CREATE INDEX ix_report_attempt_history
    ON report_generation_attempt (tenant_id, investigation_id, requested_at DESC, id DESC);

CREATE UNIQUE INDEX uk_report_attempt_active
    ON report_generation_attempt (tenant_id, investigation_id)
    WHERE status = 'STARTED';

CREATE UNIQUE INDEX uk_report_attempt_available
    ON report_generation_attempt (tenant_id, investigation_id)
    WHERE status = 'AVAILABLE';

CREATE TABLE report_claim (
    tenant_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    claim_ordinal INTEGER NOT NULL,
    statement VARCHAR(2000) NOT NULL,
    PRIMARY KEY (tenant_id, attempt_id, claim_type, claim_ordinal),
    CONSTRAINT fk_report_claim_attempt
        FOREIGN KEY (tenant_id, attempt_id)
        REFERENCES report_generation_attempt (tenant_id, id),
    CONSTRAINT ck_report_claim_type
        CHECK (claim_type IN (
            'SUMMARY', 'OBSERVATION', 'INFERENCE', 'PROBABLE_CAUSE',
            'CONFIDENCE', 'RECOMMENDATION', 'CONTRADICTION'
        )),
    CONSTRAINT ck_report_claim_ordinal CHECK (claim_ordinal >= 0),
    CONSTRAINT ck_report_claim_statement CHECK (BTRIM(statement) <> '')
);

CREATE TABLE report_claim_evidence_reference (
    tenant_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    claim_ordinal INTEGER NOT NULL,
    evidence_id UUID NOT NULL,
    PRIMARY KEY (tenant_id, attempt_id, claim_type, claim_ordinal, evidence_id),
    CONSTRAINT fk_report_claim_evidence_claim
        FOREIGN KEY (tenant_id, attempt_id, claim_type, claim_ordinal)
        REFERENCES report_claim (tenant_id, attempt_id, claim_type, claim_ordinal),
    CONSTRAINT fk_report_claim_evidence_source
        FOREIGN KEY (tenant_id, evidence_id)
        REFERENCES evidence_collection_attempt (tenant_id, id)
);

CREATE TABLE report_claim_knowledge_reference (
    tenant_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    claim_ordinal INTEGER NOT NULL,
    retrieval_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    PRIMARY KEY (tenant_id, attempt_id, claim_type, claim_ordinal, retrieval_id, chunk_id),
    CONSTRAINT fk_report_claim_knowledge_claim
        FOREIGN KEY (tenant_id, attempt_id, claim_type, claim_ordinal)
        REFERENCES report_claim (tenant_id, attempt_id, claim_type, claim_ordinal),
    CONSTRAINT fk_report_claim_knowledge_attempt
        FOREIGN KEY (tenant_id, attempt_id, retrieval_id)
        REFERENCES report_generation_attempt (tenant_id, id, retrieval_id),
    CONSTRAINT fk_report_claim_knowledge_source
        FOREIGN KEY (tenant_id, retrieval_id, chunk_id)
        REFERENCES knowledge_retrieval_result (tenant_id, retrieval_id, chunk_id)
);
