ALTER TABLE knowledge_document_version
    ADD COLUMN source_format VARCHAR(20),
    ADD COLUMN source_artifact_hash CHAR(64),
    ADD COLUMN pdf_artifact_hash CHAR(64),
    ADD COLUMN extraction_strategy_version VARCHAR(80);

UPDATE knowledge_document_version
SET source_format = 'MARKDOWN',
    source_artifact_hash = source_content_hash,
    extraction_strategy_version = 'markdown-front-matter/v1';

ALTER TABLE knowledge_document_version
    ALTER COLUMN source_format SET NOT NULL,
    ALTER COLUMN source_artifact_hash SET NOT NULL,
    ALTER COLUMN extraction_strategy_version SET NOT NULL,
    ADD CONSTRAINT ck_knowledge_document_source_format
        CHECK (
            (source_format = 'MARKDOWN' AND pdf_artifact_hash IS NULL)
            OR (
                source_format = 'PDF'
                AND pdf_artifact_hash IS NOT NULL
                AND pdf_artifact_hash ~ '^[0-9a-f]{64}$'
            )
        ),
    ADD CONSTRAINT ck_knowledge_document_source_metadata
        CHECK (
            source_artifact_hash ~ '^[0-9a-f]{64}$'
            AND BTRIM(extraction_strategy_version) <> ''
        );

ALTER TABLE knowledge_chunk
    DROP CONSTRAINT ck_knowledge_chunk_lines,
    DROP CONSTRAINT ck_knowledge_chunk_embedding,
    DROP CONSTRAINT ck_knowledge_chunk_text,
    ALTER COLUMN source_start_line DROP NOT NULL,
    ALTER COLUMN source_end_line DROP NOT NULL,
    ALTER COLUMN embedding_model_id DROP NOT NULL,
    ALTER COLUMN embedding_dimensions DROP NOT NULL,
    ALTER COLUMN embedding_normalized DROP NOT NULL,
    ALTER COLUMN embedded_at DROP NOT NULL,
    ALTER COLUMN embedding DROP NOT NULL,
    ADD COLUMN source_start_page INTEGER,
    ADD COLUMN source_end_page INTEGER,
    ADD COLUMN source_start_block INTEGER,
    ADD COLUMN source_end_block INTEGER,
    ADD CONSTRAINT ck_knowledge_chunk_locator
        CHECK (
            (
                source_start_line IS NOT NULL
                AND source_end_line IS NOT NULL
                AND source_start_line > 0
                AND source_end_line >= source_start_line
                AND source_start_page IS NULL
                AND source_end_page IS NULL
                AND source_start_block IS NULL
                AND source_end_block IS NULL
            )
            OR (
                source_start_line IS NULL
                AND source_end_line IS NULL
                AND source_start_page IS NOT NULL
                AND source_end_page IS NOT NULL
                AND source_start_page > 0
                AND source_start_page <= 15
                AND source_end_page = source_start_page
                AND source_start_block IS NOT NULL
                AND source_end_block IS NOT NULL
                AND source_start_block > 0
                AND source_end_block >= source_start_block
            )
        ),
    ADD CONSTRAINT ck_knowledge_chunk_embedding
        CHECK (
            (
                embedding_model_id IS NULL
                AND embedding_dimensions IS NULL
                AND embedding_normalized IS NULL
                AND embedded_at IS NULL
                AND embedding IS NULL
            )
            OR (
                embedding_model_id IS NOT NULL
                AND BTRIM(embedding_model_id) <> ''
                AND embedding_dimensions IS NOT NULL
                AND embedding_normalized
                AND embedded_at IS NOT NULL
                AND embedding IS NOT NULL
                AND vector_dims(embedding) = embedding_dimensions
                AND (
                    (
                        embedding_model_id = 'nomic-embed-text'
                        AND embedding_dimensions = 768
                    )
                    OR (
                        embedding_model_id = 'amazon.titan-embed-text-v2:0'
                        AND embedding_dimensions = 1024
                    )
                )
                AND ABS(vector_norm(embedding) - 1.0) <= 0.01
            )
        ),
    ADD CONSTRAINT ck_knowledge_chunk_text
        CHECK (
            BTRIM(section_path) <> ''
            AND BTRIM(raw_content) <> ''
            AND BTRIM(embedding_input) <> ''
            AND BTRIM(embedding_input_template_version) <> ''
            AND BTRIM(chunking_strategy_version) <> ''
        );

ALTER TABLE knowledge_retrieval_result
    DROP CONSTRAINT ck_knowledge_retrieval_result_document,
    ALTER COLUMN source_start_line DROP NOT NULL,
    ALTER COLUMN source_end_line DROP NOT NULL,
    ADD COLUMN source_name VARCHAR(160),
    ADD COLUMN source_format VARCHAR(20),
    ADD COLUMN pdf_artifact_hash CHAR(64),
    ADD COLUMN source_start_page INTEGER,
    ADD COLUMN source_end_page INTEGER,
    ADD COLUMN source_start_block INTEGER,
    ADD COLUMN source_end_block INTEGER;

UPDATE knowledge_retrieval_result result
SET source_name = document.source_name,
    source_format = document.source_format,
    pdf_artifact_hash = document.pdf_artifact_hash
FROM knowledge_document_version document
WHERE document.tenant_id = result.tenant_id
  AND document.id = result.document_version_id;

ALTER TABLE knowledge_retrieval_result
    ALTER COLUMN source_name SET NOT NULL,
    ALTER COLUMN source_format SET NOT NULL,
    ADD CONSTRAINT ck_knowledge_retrieval_result_document
        CHECK (
            document_type IN ('RUNBOOK', 'POLICY')
            AND approval_status = 'APPROVED'
            AND BTRIM(document_title) <> ''
            AND BTRIM(document_version) <> ''
            AND BTRIM(applies_to) <> ''
            AND BTRIM(section_path) <> ''
            AND BTRIM(raw_content) <> ''
            AND BTRIM(source_name) <> ''
            AND (
                (source_format = 'MARKDOWN' AND pdf_artifact_hash IS NULL)
                OR (
                    source_format = 'PDF'
                    AND pdf_artifact_hash IS NOT NULL
                    AND pdf_artifact_hash ~ '^[0-9a-f]{64}$'
                )
            )
        ),
    ADD CONSTRAINT ck_knowledge_retrieval_result_locator
        CHECK (
            (
                source_format = 'MARKDOWN'
                AND source_start_line IS NOT NULL
                AND source_end_line IS NOT NULL
                AND source_start_line > 0
                AND source_end_line >= source_start_line
                AND source_start_page IS NULL
                AND source_end_page IS NULL
                AND source_start_block IS NULL
                AND source_end_block IS NULL
            )
            OR (
                source_format = 'PDF'
                AND source_start_line IS NULL
                AND source_end_line IS NULL
                AND source_start_page IS NOT NULL
                AND source_end_page = source_start_page
                AND source_start_page > 0
                AND source_start_page <= 15
                AND source_start_block IS NOT NULL
                AND source_end_block IS NOT NULL
                AND source_start_block > 0
                AND source_end_block >= source_start_block
            )
        );
