ALTER TABLE knowledge_chunk
    DROP CONSTRAINT ck_knowledge_chunk_embedding;

ALTER TABLE knowledge_chunk
    ALTER COLUMN embedding TYPE vector
    USING embedding::vector;

ALTER TABLE knowledge_chunk
    ADD CONSTRAINT ck_knowledge_chunk_embedding
    CHECK (
        embedding_normalized
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
    );

ALTER TABLE knowledge_retrieval_attempt
    DROP CONSTRAINT ck_knowledge_retrieval_parameters;

ALTER TABLE knowledge_retrieval_attempt
    ADD CONSTRAINT ck_knowledge_retrieval_parameters
    CHECK (
        BTRIM(query_text) <> ''
        AND BTRIM(query_template_version) <> ''
        AND BTRIM(embedding_model_id) <> ''
        AND BTRIM(ranking_version) <> ''
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
        AND rrf_k > 0
        AND candidate_depth > 0
        AND minimum_lexical_rank >= 0
        AND minimum_vector_similarity >= -1
        AND minimum_vector_similarity <= 1
    );
