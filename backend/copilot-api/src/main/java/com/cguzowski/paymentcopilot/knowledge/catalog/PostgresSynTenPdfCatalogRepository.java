package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PostgresSynTenPdfCatalogRepository implements SynTenPdfCatalogRepository {

    private final JdbcClient jdbcClient;

    PostgresSynTenPdfCatalogRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PdfCatalogImportSummary importAll(List<PdfCatalogDocumentPlan> plans) {
        int importedDocuments = 0;
        int skippedDocuments = 0;
        int cataloguedChunks = 0;
        for (PdfCatalogDocumentPlan plan : plans) {
            SynTenPdfSourceDocument source = plan.document().source();
            Optional<String> existingHash = jdbcClient
                    .sql("""
                            SELECT source_content_hash
                            FROM knowledge_document_version
                            WHERE tenant_id = :tenantId
                              AND document_id = :documentId
                              AND document_version = :version
                            """)
                    .param("tenantId", source.tenantId())
                    .param("documentId", source.documentId())
                    .param("version", source.version())
                    .query(String.class)
                    .optional();
            if (existingHash.isPresent()) {
                if (!existingHash.orElseThrow().equals(plan.catalogContentHash())) {
                    throw new IllegalArgumentException(
                            "Changed SynTen PDF catalog content requires a new document version: "
                                    + source.documentKey());
                }
                skippedDocuments++;
                continue;
            }
            insertDocument(plan);
            for (PdfKnowledgeChunkDraft chunk : plan.chunks()) {
                insertChunk(plan, chunk);
            }
            importedDocuments++;
            cataloguedChunks += plan.chunks().size();
        }
        return new PdfCatalogImportSummary(importedDocuments, skippedDocuments, cataloguedChunks);
    }

    private void insertDocument(PdfCatalogDocumentPlan plan) {
        SynTenPdfSourceDocument source = plan.document().source();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_document_version (
                            id, tenant_id, document_id, document_type, title,
                            document_version, incident_family, applies_to,
                            approval_status, approved_by, approved_at, effective_at,
                            source_name, source_content_hash, source_format,
                            source_artifact_hash, pdf_artifact_hash,
                            extraction_strategy_version, imported_at
                        ) VALUES (
                            :id, :tenantId, :documentId, :documentType, :title,
                            :documentVersion, :incidentFamily, :appliesTo,
                            :approvalStatus, :approvedBy, :approvedAt, :effectiveAt,
                            :sourceName, :sourceContentHash, 'PDF',
                            :sourceArtifactHash, :pdfArtifactHash,
                            :extractionStrategyVersion, :importedAt
                        )
                        """)
                .param("id", plan.documentVersionId())
                .param("tenantId", source.tenantId())
                .param("documentId", source.documentId())
                .param("documentType", source.type().name())
                .param("title", source.title())
                .param("documentVersion", source.version())
                .param("incidentFamily", source.incidentFamily())
                .param("appliesTo", source.appliesTo())
                .param("approvalStatus", source.approvalStatus().name())
                .param("approvedBy", source.approvedBy())
                .param("approvedAt", utc(source.approvedAt()))
                .param("effectiveAt", utc(source.effectiveAt()))
                .param("sourceName", source.pdfName())
                .param("sourceContentHash", plan.catalogContentHash())
                .param("sourceArtifactHash", source.sourceSha256())
                .param("pdfArtifactHash", source.pdfSha256())
                .param("extractionStrategyVersion", plan.document().extractionStrategyVersion())
                .param("importedAt", utc(plan.importedAt()))
                .update();
    }

    private void insertChunk(PdfCatalogDocumentPlan plan, PdfKnowledgeChunkDraft chunk) {
        SynTenPdfSourceDocument source = plan.document().source();
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_chunk (
                            id, tenant_id, document_version_id, chunk_ordinal,
                            section_path, raw_content, embedding_input,
                            raw_content_hash, embedding_input_hash,
                            embedding_input_template_version, chunking_strategy_version,
                            source_start_line, source_end_line,
                            source_start_page, source_end_page,
                            source_start_block, source_end_block, estimated_tokens,
                            embedding_model_id, embedding_dimensions,
                            embedding_normalized, embedded_at, embedding
                        ) VALUES (
                            :id, :tenantId, :documentVersionId, :ordinal,
                            :sectionPath, :rawContent, :embeddingInput,
                            :rawContentHash, :embeddingInputHash,
                            :embeddingInputTemplateVersion, :chunkingStrategyVersion,
                            NULL, NULL, :pageNumber, :pageNumber,
                            :startBlock, :endBlock, :estimatedTokens,
                            NULL, NULL, NULL, NULL, NULL
                        )
                        """)
                .param("id", chunk.chunkId())
                .param("tenantId", source.tenantId())
                .param("documentVersionId", plan.documentVersionId())
                .param("ordinal", chunk.ordinal())
                .param("sectionPath", chunk.sectionPath())
                .param("rawContent", chunk.rawContent())
                .param("embeddingInput", chunk.embeddingInput())
                .param("rawContentHash", chunk.rawContentHash())
                .param("embeddingInputHash", chunk.embeddingInputHash())
                .param("embeddingInputTemplateVersion", chunk.embeddingInputTemplateVersion())
                .param("chunkingStrategyVersion", chunk.chunkingStrategyVersion())
                .param("pageNumber", chunk.pageNumber())
                .param("startBlock", chunk.startBlock())
                .param("endBlock", chunk.endBlock())
                .param("estimatedTokens", chunk.estimatedTokens())
                .update();
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
