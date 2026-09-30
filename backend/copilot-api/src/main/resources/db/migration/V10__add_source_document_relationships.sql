ALTER TABLE knowledge_document_version
    ADD COLUMN document_key VARCHAR(80),
    ADD COLUMN related_document_keys TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN relationship_metadata_version VARCHAR(80),
    ADD CONSTRAINT document_relationship_metadata_tuple CHECK (
        (document_key IS NULL AND relationship_metadata_version IS NULL AND cardinality(related_document_keys) = 0)
        OR (document_key IS NOT NULL AND relationship_metadata_version = 'document-relationships/v1'
            AND cardinality(related_document_keys) <= 32)
    );

CREATE INDEX knowledge_document_relationship_key_idx
    ON knowledge_document_version (tenant_id, document_key);

ALTER TABLE knowledge_retrieval_result ADD COLUMN ranking_evidence JSONB;
