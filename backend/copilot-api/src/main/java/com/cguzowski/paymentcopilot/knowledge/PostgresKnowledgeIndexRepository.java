package com.cguzowski.paymentcopilot.knowledge;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PostgresKnowledgeIndexRepository implements KnowledgeIndexRepository {

    static final String CHUNKING_STRATEGY_VERSION = "markdown-sections/v1";

    private final JdbcClient jdbcClient;

    PostgresKnowledgeIndexRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<String> findSourceContentHash(UUID tenantId, UUID documentId, String version) {
        return jdbcClient.sql("""
                        SELECT source_content_hash
                        FROM knowledge_document_version
                        WHERE tenant_id = :tenantId
                          AND document_id = :documentId
                          AND document_version = :version
                        """)
                .param("tenantId", tenantId)
                .param("documentId", documentId)
                .param("version", version)
                .query(String.class)
                .optional();
    }

    @Override
    public boolean insert(IndexedKnowledgeDocument indexed) {
        ApprovedKnowledgeDocument document = indexed.document();
        int inserted = jdbcClient.sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, :documentType, :title,
                            :documentVersion, :incidentFamily, :appliesTo,
                            :approvalStatus, :approvedBy, :approvedAt, :effectiveAt,
                            :sourceName, :sourceContentHash, :importedAt
                        )
                        ON CONFLICT (tenant_id, document_id, document_version) DO NOTHING
                        """)
                .param("id", indexed.id())
                .param("tenantId", document.tenantId())
                .param("documentId", document.documentId())
                .param("documentType", document.type().name())
                .param("title", document.title())
                .param("documentVersion", document.version())
                .param("incidentFamily", document.incidentFamily())
                .param("appliesTo", document.appliesTo())
                .param("approvalStatus", document.approvalStatus().name())
                .param("approvedBy", document.approvedBy())
                .param("approvedAt", utc(document.approvedAt()))
                .param("effectiveAt", utc(document.effectiveAt()))
                .param("sourceName", document.sourceName())
                .param("sourceContentHash", indexed.sourceContentHash())
                .param("importedAt", utc(indexed.importedAt()))
                .update();
        if (inserted == 0) {
            return false;
        }
        for (IndexedKnowledgeChunk chunk : indexed.chunks()) {
            insertChunk(indexed, chunk);
        }
        return true;
    }

    private void insertChunk(IndexedKnowledgeDocument indexed, IndexedKnowledgeChunk indexedChunk) {
        ApprovedKnowledgeDocument document = indexed.document();
        KnowledgeChunkDraft chunk = indexedChunk.draft();
        KnowledgeEmbedding embedding = indexedChunk.embedding();
        jdbcClient.sql("""
                        INSERT INTO knowledge_chunk (
                            id, tenant_id, document_version_id, chunk_ordinal,
                            section_path, raw_content, embedding_input,
                            raw_content_hash, embedding_input_hash,
                            embedding_input_template_version, chunking_strategy_version,
                            source_start_line, source_end_line, estimated_tokens,
                            embedding_model_id, embedding_dimensions,
                            embedding_normalized, embedded_at, embedding
                        ) VALUES (
                            :id, :tenantId, :documentVersionId, :chunkOrdinal,
                            :sectionPath, :rawContent, :embeddingInput,
                            :rawContentHash, :embeddingInputHash,
                            :embeddingInputTemplateVersion, :chunkingStrategyVersion,
                            :sourceStartLine, :sourceEndLine, :estimatedTokens,
                            :embeddingModelId, :embeddingDimensions,
                            :embeddingNormalized, :embeddedAt, CAST(:embedding AS vector)
                        )
                        """)
                .param("id", indexedChunk.id())
                .param("tenantId", document.tenantId())
                .param("documentVersionId", indexed.id())
                .param("chunkOrdinal", chunk.ordinal())
                .param("sectionPath", chunk.sectionPath())
                .param("rawContent", chunk.rawContent())
                .param("embeddingInput", chunk.embeddingInput())
                .param("rawContentHash", chunk.rawContentHash())
                .param("embeddingInputHash", chunk.embeddingInputHash())
                .param("embeddingInputTemplateVersion", chunk.embeddingInputTemplateVersion())
                .param("chunkingStrategyVersion", CHUNKING_STRATEGY_VERSION)
                .param("sourceStartLine", chunk.sourceStartLine())
                .param("sourceEndLine", chunk.sourceEndLine())
                .param("estimatedTokens", chunk.estimatedTokens())
                .param("embeddingModelId", embedding.modelId())
                .param("embeddingDimensions", embedding.dimensions())
                .param("embeddingNormalized", embedding.normalized())
                .param("embeddedAt", utc(indexedChunk.embeddedAt()))
                .param("embedding", vectorLiteral(embedding.vector()))
                .update();
    }

    static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(Float.toString(vector[index]));
        }
        return value.append(']').toString();
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
