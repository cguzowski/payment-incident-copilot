CREATE TABLE knowledge_document_version (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    document_id UUID NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    title VARCHAR(160) NOT NULL,
    document_version VARCHAR(40) NOT NULL,
    incident_family VARCHAR(80) NOT NULL,
    applies_to VARCHAR(120) NOT NULL,
    approval_status VARCHAR(20) NOT NULL,
    approved_by UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    source_name VARCHAR(160) NOT NULL,
    source_content_hash CHAR(64) NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_knowledge_document_version
        UNIQUE (tenant_id, document_id, document_version),
    CONSTRAINT uk_knowledge_document_tenant_id
        UNIQUE (tenant_id, id),
    CONSTRAINT uk_knowledge_document_tenant_version_document
        UNIQUE (tenant_id, id, document_id),
    CONSTRAINT ck_knowledge_document_type
        CHECK (document_type IN ('RUNBOOK', 'POLICY')),
    CONSTRAINT ck_knowledge_document_approval
        CHECK (approval_status IN ('DRAFT', 'APPROVED', 'SUPERSEDED')),
    CONSTRAINT ck_knowledge_document_text
        CHECK (
            BTRIM(title) <> ''
            AND BTRIM(document_version) <> ''
            AND BTRIM(incident_family) <> ''
            AND BTRIM(applies_to) <> ''
            AND BTRIM(source_name) <> ''
        ),
    CONSTRAINT ck_knowledge_document_hash
        CHECK (source_content_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_knowledge_document_eligibility
    ON knowledge_document_version (
        tenant_id,
        incident_family,
        approval_status,
        effective_at,
        document_type
    );

CREATE TABLE knowledge_chunk (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    chunk_ordinal INTEGER NOT NULL,
    section_path VARCHAR(500) NOT NULL,
    raw_content TEXT NOT NULL,
    embedding_input TEXT NOT NULL,
    raw_content_hash CHAR(64) NOT NULL,
    embedding_input_hash CHAR(64) NOT NULL,
    embedding_input_template_version VARCHAR(80) NOT NULL,
    chunking_strategy_version VARCHAR(80) NOT NULL,
    source_start_line INTEGER NOT NULL,
    source_end_line INTEGER NOT NULL,
    estimated_tokens INTEGER NOT NULL,
    embedding_model_id VARCHAR(120) NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    embedding_normalized BOOLEAN NOT NULL,
    embedded_at TIMESTAMPTZ NOT NULL,
    embedding vector(1024) NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        SETWEIGHT(TO_TSVECTOR('english', COALESCE(section_path, '')), 'B')
        || SETWEIGHT(TO_TSVECTOR('english', COALESCE(raw_content, '')), 'C')
    ) STORED,
    CONSTRAINT uk_knowledge_chunk_document_ordinal
        UNIQUE (tenant_id, document_version_id, chunk_ordinal),
    CONSTRAINT uk_knowledge_chunk_tenant_id_document
        UNIQUE (tenant_id, id, document_version_id),
    CONSTRAINT fk_knowledge_chunk_document
        FOREIGN KEY (tenant_id, document_version_id)
        REFERENCES knowledge_document_version (tenant_id, id),
    CONSTRAINT ck_knowledge_chunk_lines
        CHECK (source_start_line > 0 AND source_end_line >= source_start_line),
    CONSTRAINT ck_knowledge_chunk_tokens
        CHECK (estimated_tokens > 0 AND estimated_tokens <= 600),
    CONSTRAINT ck_knowledge_chunk_embedding
        CHECK (
            embedding_dimensions = 1024
            AND embedding_normalized
            AND ABS(vector_norm(embedding) - 1.0) <= 0.01
        ),
    CONSTRAINT ck_knowledge_chunk_text
        CHECK (
            BTRIM(section_path) <> ''
            AND BTRIM(raw_content) <> ''
            AND BTRIM(embedding_input) <> ''
            AND BTRIM(embedding_input_template_version) <> ''
            AND BTRIM(chunking_strategy_version) <> ''
            AND BTRIM(embedding_model_id) <> ''
        ),
    CONSTRAINT ck_knowledge_chunk_hashes
        CHECK (
            raw_content_hash ~ '^[0-9a-f]{64}$'
            AND embedding_input_hash ~ '^[0-9a-f]{64}$'
        )
);

CREATE INDEX ix_knowledge_chunk_full_text
    ON knowledge_chunk USING GIN (search_vector);

CREATE TABLE knowledge_retrieval_attempt (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    investigation_id UUID NOT NULL,
    investigation_correlation_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    query_text VARCHAR(2000) NOT NULL,
    query_template_version VARCHAR(80) NOT NULL,
    contributing_evidence_ids UUID[] NOT NULL DEFAULT '{}',
    embedding_model_id VARCHAR(120) NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    metadata_filters JSONB NOT NULL,
    ranking_version VARCHAR(80) NOT NULL,
    rrf_k INTEGER NOT NULL,
    candidate_depth INTEGER NOT NULL,
    minimum_lexical_rank REAL NOT NULL,
    minimum_vector_similarity REAL NOT NULL,
    status_detail VARCHAR(500),
    CONSTRAINT uk_knowledge_retrieval_tenant_id
        UNIQUE (tenant_id, id),
    CONSTRAINT fk_knowledge_retrieval_investigation
        FOREIGN KEY (
            tenant_id,
            investigation_id,
            investigation_correlation_id
        ) REFERENCES investigation (tenant_id, id, correlation_id),
    CONSTRAINT ck_knowledge_retrieval_status
        CHECK (status IN (
            'STARTED',
            'AVAILABLE',
            'PARTIAL',
            'NO_MATCH',
            'UNAVAILABLE',
            'TIMED_OUT',
            'MALFORMED'
        )),
    CONSTRAINT ck_knowledge_retrieval_completion
        CHECK (
            (status = 'STARTED' AND completed_at IS NULL)
            OR (status <> 'STARTED' AND completed_at IS NOT NULL)
        ),
    CONSTRAINT ck_knowledge_retrieval_parameters
        CHECK (
            BTRIM(query_text) <> ''
            AND BTRIM(query_template_version) <> ''
            AND BTRIM(embedding_model_id) <> ''
            AND BTRIM(ranking_version) <> ''
            AND embedding_dimensions = 1024
            AND rrf_k > 0
            AND candidate_depth > 0
            AND minimum_lexical_rank >= 0
            AND minimum_vector_similarity >= -1
            AND minimum_vector_similarity <= 1
        )
);

CREATE INDEX ix_knowledge_retrieval_history
    ON knowledge_retrieval_attempt (
        tenant_id,
        investigation_id,
        requested_at DESC,
        id DESC
    );

CREATE TABLE knowledge_retrieval_result (
    retrieval_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    document_version_id UUID NOT NULL,
    document_id UUID NOT NULL,
    selected_position INTEGER NOT NULL,
    lexical_rank REAL,
    lexical_position INTEGER,
    vector_similarity REAL,
    vector_distance REAL,
    vector_position INTEGER,
    fused_position INTEGER NOT NULL,
    fused_score DOUBLE PRECISION NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    document_title VARCHAR(160) NOT NULL,
    document_version VARCHAR(40) NOT NULL,
    applies_to VARCHAR(160) NOT NULL,
    section_path VARCHAR(500) NOT NULL,
    raw_content TEXT NOT NULL,
    source_start_line INTEGER NOT NULL,
    source_end_line INTEGER NOT NULL,
    approval_status VARCHAR(20) NOT NULL,
    approved_by UUID NOT NULL,
    approved_at TIMESTAMPTZ NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (retrieval_id, chunk_id),
    CONSTRAINT uk_knowledge_retrieval_selected_position
        UNIQUE (retrieval_id, selected_position),
    CONSTRAINT fk_knowledge_retrieval_result_attempt
        FOREIGN KEY (tenant_id, retrieval_id)
        REFERENCES knowledge_retrieval_attempt (tenant_id, id),
    CONSTRAINT fk_knowledge_retrieval_result_chunk
        FOREIGN KEY (tenant_id, chunk_id, document_version_id)
        REFERENCES knowledge_chunk (tenant_id, id, document_version_id),
    CONSTRAINT fk_knowledge_retrieval_result_document
        FOREIGN KEY (tenant_id, document_version_id, document_id)
        REFERENCES knowledge_document_version (tenant_id, id, document_id),
    CONSTRAINT ck_knowledge_retrieval_result_position
        CHECK (
            selected_position > 0
            AND fused_position > 0
            AND (lexical_position IS NULL OR lexical_position > 0)
            AND (vector_position IS NULL OR vector_position > 0)
            AND (lexical_position IS NOT NULL OR vector_position IS NOT NULL)
        ),
    CONSTRAINT ck_knowledge_retrieval_result_values
        CHECK (
            fused_score > 0
            AND (lexical_rank IS NULL OR lexical_rank > 0)
            AND (vector_similarity IS NULL OR (
                vector_similarity >= -1 AND vector_similarity <= 1
            ))
            AND (vector_distance IS NULL OR (
                vector_distance >= 0 AND vector_distance <= 2
            ))
            AND ((vector_similarity IS NULL AND vector_distance IS NULL) OR (
                vector_similarity IS NOT NULL
                AND vector_distance IS NOT NULL
                AND ABS(vector_distance - (1 - vector_similarity)) <= 0.0001
            ))
        ),
    CONSTRAINT ck_knowledge_retrieval_result_document
        CHECK (
            document_type IN ('RUNBOOK', 'POLICY')
            AND approval_status = 'APPROVED'
            AND BTRIM(document_title) <> ''
            AND BTRIM(document_version) <> ''
            AND BTRIM(applies_to) <> ''
            AND BTRIM(section_path) <> ''
            AND BTRIM(raw_content) <> ''
            AND source_start_line > 0
            AND source_end_line >= source_start_line
        )
);
