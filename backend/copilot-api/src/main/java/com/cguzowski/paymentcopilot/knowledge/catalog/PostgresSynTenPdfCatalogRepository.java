package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PostgresSynTenPdfCatalogRepository implements SynTenPdfCatalogRepository, SynTenPdfCatalogSnapshotRepository {

    private final JdbcClient jdbcClient;

    PostgresSynTenPdfCatalogRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PdfCatalogImportSummary importAll(SynTenPdfCatalogPlan catalogPlan, Instant importedAt)
            throws IllegalArgumentException {
        int importedDocuments = 0;
        int skippedDocuments = 0;
        int cataloguedChunks = 0;
        for (PdfCatalogDocumentPlan plan : catalogPlan.documents()) {
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
            insertDocument(plan, importedAt);
            for (PdfKnowledgeChunkDraft chunk : plan.chunks()) {
                insertChunk(plan, chunk);
            }
            importedDocuments++;
            cataloguedChunks += plan.chunks().size();
        }
        return new PdfCatalogImportSummary(importedDocuments, skippedDocuments, cataloguedChunks);
    }

    @Override
    public SynTenPdfEmbeddingCatalogSnapshot readEmbeddingSnapshot(SynTenPdfCatalogPlan expectedPlan)
            throws IllegalStateException {
        List<PersistedCatalogRow> persistedRows = jdbcClient
                .sql("""
                        SELECT document.id AS document_version_id,
                               document.document_id,
                               document.document_type,
                               document.title,
                               document.document_version,
                               document.incident_family,
                               document.applies_to,
                               document.approval_status,
                               document.approved_by,
                               document.approved_at,
                               document.effective_at,
                               document.source_name,
                               document.source_content_hash,
                               document.source_format,
                               document.source_artifact_hash,
                               document.pdf_artifact_hash,
                               document.extraction_strategy_version,
                               chunk.id AS chunk_id,
                               chunk.chunk_ordinal,
                               chunk.section_path,
                               chunk.raw_content,
                               chunk.embedding_input,
                               chunk.raw_content_hash,
                               chunk.embedding_input_hash,
                               chunk.embedding_input_template_version,
                               chunk.chunking_strategy_version,
                               chunk.source_start_line,
                               chunk.source_end_line,
                               chunk.source_start_page,
                               chunk.source_end_page,
                               chunk.source_start_block,
                               chunk.source_end_block,
                               chunk.estimated_tokens,
                               chunk.embedding_model_id,
                               chunk.embedding_dimensions,
                               chunk.embedding_normalized,
                               chunk.embedded_at,
                               vector_dims(chunk.embedding) AS vector_dimensions,
                               vector_norm(chunk.embedding) AS vector_norm
                        FROM knowledge_document_version document
                        LEFT JOIN knowledge_chunk chunk
                          ON chunk.tenant_id = document.tenant_id
                         AND chunk.document_version_id = document.id
                        WHERE document.tenant_id = :tenantId
                        ORDER BY document.document_id, document.document_version, chunk.chunk_ordinal
                        """)
                .param("tenantId", expectedPlan.tenantId())
                .query((resultSet, rowNumber) -> new PersistedCatalogRow(
                        resultSet.getObject("document_version_id", UUID.class),
                        resultSet.getObject("document_id", UUID.class),
                        resultSet.getString("document_type"),
                        resultSet.getString("title"),
                        resultSet.getString("document_version"),
                        resultSet.getString("incident_family"),
                        resultSet.getString("applies_to"),
                        resultSet.getString("approval_status"),
                        resultSet.getObject("approved_by", UUID.class),
                        instant(resultSet.getObject("approved_at", OffsetDateTime.class)),
                        instant(resultSet.getObject("effective_at", OffsetDateTime.class)),
                        resultSet.getString("source_name"),
                        resultSet.getString("source_content_hash"),
                        resultSet.getString("source_format"),
                        resultSet.getString("source_artifact_hash"),
                        resultSet.getString("pdf_artifact_hash"),
                        resultSet.getString("extraction_strategy_version"),
                        resultSet.getObject("chunk_id", UUID.class),
                        resultSet.getObject("chunk_ordinal", Integer.class),
                        resultSet.getString("section_path"),
                        resultSet.getString("raw_content"),
                        resultSet.getString("embedding_input"),
                        resultSet.getString("raw_content_hash"),
                        resultSet.getString("embedding_input_hash"),
                        resultSet.getString("embedding_input_template_version"),
                        resultSet.getString("chunking_strategy_version"),
                        resultSet.getObject("source_start_line", Integer.class),
                        resultSet.getObject("source_end_line", Integer.class),
                        resultSet.getObject("source_start_page", Integer.class),
                        resultSet.getObject("source_end_page", Integer.class),
                        resultSet.getObject("source_start_block", Integer.class),
                        resultSet.getObject("source_end_block", Integer.class),
                        resultSet.getObject("estimated_tokens", Integer.class),
                        resultSet.getString("embedding_model_id"),
                        resultSet.getObject("embedding_dimensions", Integer.class),
                        resultSet.getObject("embedding_normalized", Boolean.class),
                        instant(resultSet.getObject("embedded_at", OffsetDateTime.class)),
                        resultSet.getObject("vector_dimensions", Integer.class),
                        resultSet.getObject("vector_norm", Double.class)))
                .list();
        return compareWithExpectedPlan(expectedPlan, persistedRows);
    }

    private static SynTenPdfEmbeddingCatalogSnapshot compareWithExpectedPlan(
            SynTenPdfCatalogPlan expectedPlan, List<PersistedCatalogRow> persistedRows) {
        Map<DocumentIdentity, PdfCatalogDocumentPlan> expectedDocuments = new HashMap<>();
        Map<UUID, ExpectedChunk> expectedChunks = new HashMap<>();
        for (PdfCatalogDocumentPlan document : expectedPlan.documents()) {
            SynTenPdfSourceDocument source = document.document().source();
            expectedDocuments.put(new DocumentIdentity(source.documentId(), source.version()), document);
            for (PdfKnowledgeChunkDraft chunk : document.chunks()) {
                expectedChunks.put(chunk.chunkId(), new ExpectedChunk(document, chunk));
            }
        }

        Set<UUID> seenDocuments = new HashSet<>();
        Set<UUID> seenChunks = new HashSet<>();
        List<SynTenPdfEmbeddingMetadata> embeddingMetadata = new ArrayList<>();
        for (PersistedCatalogRow row : persistedRows) {
            DocumentIdentity identity = new DocumentIdentity(row.documentId(), row.documentVersion());
            PdfCatalogDocumentPlan expectedDocument = expectedDocuments.get(identity);
            if (!KnowledgeSourceFormat.PDF.name().equals(row.sourceFormat())) {
                if (expectedDocument != null) {
                    throw catalogDrift("expected document is not stored as PDF");
                }
                continue;
            }
            if (expectedDocument == null) {
                throw catalogDrift("unexpected PDF document version");
            }
            compareDocument(expectedDocument, row);
            seenDocuments.add(row.documentVersionId());
            if (row.chunkId() == null) {
                throw catalogDrift("PDF document has no chunks");
            }
            ExpectedChunk expectedChunk = expectedChunks.get(row.chunkId());
            if (expectedChunk == null || expectedChunk.document() != expectedDocument) {
                throw catalogDrift("unexpected PDF chunk");
            }
            compareChunk(expectedChunk.chunk(), row);
            if (!seenChunks.add(row.chunkId())) {
                throw catalogDrift("duplicate PDF chunk");
            }
            embeddingMetadata.add(row.embeddingMetadata());
        }

        if (seenDocuments.size() != expectedDocuments.size()
                || seenChunks.size() != expectedChunks.size()
                || embeddingMetadata.size() != expectedPlan.chunkCount()) {
            throw catalogDrift("missing PDF document or chunk");
        }

        SynTenPdfEmbeddingState state = SynTenPdfEmbeddingStateMachine.classify(embeddingMetadata);
        Instant embeddedAt = state == SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL
                ? embeddingMetadata.getFirst().embeddedAt()
                : null;
        return new SynTenPdfEmbeddingCatalogSnapshot(
                expectedPlan.catalogFingerprint(), state, expectedPlan.embeddingTargets(), embeddedAt);
    }

    private static void compareDocument(PdfCatalogDocumentPlan expected, PersistedCatalogRow actual) {
        SynTenPdfSourceDocument source = expected.document().source();
        requireEqual("document version ID", expected.documentVersionId(), actual.documentVersionId());
        requireEqual("document ID", source.documentId(), actual.documentId());
        requireEqual("document type", source.type().name(), actual.documentType());
        requireEqual("title", source.title(), actual.title());
        requireEqual("document version", source.version(), actual.documentVersion());
        requireEqual("incident family", source.incidentFamily(), actual.incidentFamily());
        requireEqual("applies to", source.appliesTo(), actual.appliesTo());
        requireEqual("approval status", source.approvalStatus().name(), actual.approvalStatus());
        requireEqual("approved by", source.approvedBy(), actual.approvedBy());
        requireEqual("approved at", source.approvedAt(), actual.approvedAt());
        requireEqual("effective at", source.effectiveAt(), actual.effectiveAt());
        requireEqual("source name", source.pdfName(), actual.sourceName());
        requireEqual("catalog content hash", expected.catalogContentHash(), actual.sourceContentHash());
        requireEqual("source format", KnowledgeSourceFormat.PDF.name(), actual.sourceFormat());
        requireEqual("source artifact hash", source.sourceSha256(), actual.sourceArtifactHash());
        requireEqual("PDF artifact hash", source.pdfSha256(), actual.pdfArtifactHash());
        requireEqual(
                "extraction strategy",
                expected.document().extractionStrategyVersion(),
                actual.extractionStrategyVersion());
    }

    private static void compareChunk(PdfKnowledgeChunkDraft expected, PersistedCatalogRow actual) {
        requireEqual("chunk ID", expected.chunkId(), actual.chunkId());
        requireEqual("chunk ordinal", expected.ordinal(), actual.chunkOrdinal());
        requireEqual("section path", expected.sectionPath(), actual.sectionPath());
        requireEqual("raw content", expected.rawContent(), actual.rawContent());
        requireEqual("embedding input", expected.embeddingInput(), actual.embeddingInput());
        requireEqual("raw content hash", expected.rawContentHash(), actual.rawContentHash());
        requireEqual("embedding input hash", expected.embeddingInputHash(), actual.embeddingInputHash());
        requireEqual(
                "embedding input template",
                expected.embeddingInputTemplateVersion(),
                actual.embeddingInputTemplateVersion());
        requireEqual("chunking strategy", expected.chunkingStrategyVersion(), actual.chunkingStrategyVersion());
        requireEqual("source start line", null, actual.sourceStartLine());
        requireEqual("source end line", null, actual.sourceEndLine());
        requireEqual("source start page", expected.pageNumber(), actual.sourceStartPage());
        requireEqual("source end page", expected.pageNumber(), actual.sourceEndPage());
        requireEqual("source start block", expected.startBlock(), actual.sourceStartBlock());
        requireEqual("source end block", expected.endBlock(), actual.sourceEndBlock());
        requireEqual("estimated tokens", expected.estimatedTokens(), actual.estimatedTokens());
    }

    private static void requireEqual(String field, Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw catalogDrift(field + " mismatch");
        }
    }

    private static IllegalStateException catalogDrift(String detail) {
        return new IllegalStateException(
                "Persisted SynTen PDF catalog differs from the accepted K3 plan: " + detail + ".");
    }

    private void insertDocument(PdfCatalogDocumentPlan plan, Instant importedAt) {
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
                .param("importedAt", utc(importedAt))
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

    private record DocumentIdentity(UUID documentId, String documentVersion) {}

    private record ExpectedChunk(PdfCatalogDocumentPlan document, PdfKnowledgeChunkDraft chunk) {}

    private record PersistedCatalogRow(
            UUID documentVersionId,
            UUID documentId,
            String documentType,
            String title,
            String documentVersion,
            String incidentFamily,
            String appliesTo,
            String approvalStatus,
            UUID approvedBy,
            Instant approvedAt,
            Instant effectiveAt,
            String sourceName,
            String sourceContentHash,
            String sourceFormat,
            String sourceArtifactHash,
            String pdfArtifactHash,
            String extractionStrategyVersion,
            UUID chunkId,
            Integer chunkOrdinal,
            String sectionPath,
            String rawContent,
            String embeddingInput,
            String rawContentHash,
            String embeddingInputHash,
            String embeddingInputTemplateVersion,
            String chunkingStrategyVersion,
            Integer sourceStartLine,
            Integer sourceEndLine,
            Integer sourceStartPage,
            Integer sourceEndPage,
            Integer sourceStartBlock,
            Integer sourceEndBlock,
            Integer estimatedTokens,
            String embeddingModelId,
            Integer embeddingDimensions,
            Boolean embeddingNormalized,
            Instant embeddedAt,
            Integer vectorDimensions,
            Double vectorNorm) {

        SynTenPdfEmbeddingMetadata embeddingMetadata() {
            return new SynTenPdfEmbeddingMetadata(
                    embeddingModelId,
                    embeddingDimensions,
                    embeddingNormalized,
                    embeddedAt,
                    vectorDimensions,
                    vectorNorm);
        }
    }

    private static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
