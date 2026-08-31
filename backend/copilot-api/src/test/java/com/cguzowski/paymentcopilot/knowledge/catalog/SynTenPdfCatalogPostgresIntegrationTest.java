package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SynTenPdfCatalogPostgresIntegrationTest {

    private static final Path CORPUS_ROOT =
            Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("payment_copilot")
            .withUsername("payment_copilot")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.knowledge.pdf-catalog.corpus-root", CORPUS_ROOT::toString);
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private SynTenPdfCatalogImportService importService;

    @Autowired
    private SynTenCorpusSourceRepository sources;

    @BeforeEach
    void clearCatalog() {
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
    }

    @Test
    void catalogsThirtyVersionsWithStablePdfLocators() {
        PdfCatalogImportSummary first = importService.importCorpus();

        assertThat(first.importedDocuments()).isEqualTo(30);
        assertThat(first.skippedDocuments()).isZero();
        assertThat(first.cataloguedChunks()).isGreaterThan(30);
        assertThat(jdbcClient
                        .sql("""
                                SELECT approval_status, COUNT(*) AS documents
                                FROM knowledge_document_version
                                WHERE tenant_id = :tenantId
                                GROUP BY approval_status
                                ORDER BY approval_status
                                """)
                        .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                        .query()
                        .listOfRows())
                .containsExactly(
                        Map.of("approval_status", "APPROVED", "documents", 27L),
                        Map.of("approval_status", "SUPERSEDED", "documents", 3L));
        assertThat(jdbcClient
                        .sql("""
                                SELECT COUNT(*)
                                FROM knowledge_chunk chunk
                                JOIN knowledge_document_version document
                                  ON document.tenant_id = chunk.tenant_id
                                 AND document.id = chunk.document_version_id
                                WHERE document.tenant_id = :tenantId
                                  AND document.source_format = 'PDF'
                                  AND document.source_artifact_hash ~ '^[0-9a-f]{64}$'
                                  AND document.pdf_artifact_hash ~ '^[0-9a-f]{64}$'
                                  AND chunk.source_start_line IS NULL
                                  AND chunk.source_end_line IS NULL
                                  AND chunk.source_start_page = chunk.source_end_page
                                  AND chunk.source_start_page BETWEEN 1 AND 15
                                  AND chunk.source_start_block > 0
                                  AND chunk.source_end_block >= chunk.source_start_block
                                  AND chunk.embedding_model_id IS NULL
                                  AND chunk.embedding_dimensions IS NULL
                                  AND chunk.embedding_normalized IS NULL
                                  AND chunk.embedded_at IS NULL
                                  AND chunk.embedding IS NULL
                                """)
                        .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                        .query(Long.class)
                        .single())
                .isEqualTo((long) first.cataloguedChunks());
        List<Map<String, Object>> initialIdentity = catalogIdentity();

        PdfCatalogImportSummary repeat = importService.importCorpus();

        assertThat(repeat).isEqualTo(new PdfCatalogImportSummary(0, 30, 0));
        assertThat(catalogIdentity()).isEqualTo(initialIdentity);
    }

    @Test
    void rollsBackEveryEarlierInsertWhenTheFinalVersionConflicts() {
        SynTenPdfSourceDocument finalSource = sources.findAll().getLast();
        insertConflictingVersion(finalSource);

        assertThatThrownBy(importService::importCorpus)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a new document version")
                .hasMessageContaining(finalSource.documentKey());
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM knowledge_document_version")
                        .query(Long.class)
                        .single())
                .isEqualTo(1L);
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM knowledge_chunk")
                        .query(Long.class)
                        .single())
                .isZero();
    }

    private List<Map<String, Object>> catalogIdentity() {
        return jdbcClient
                .sql("""
                        SELECT document.id AS document_version_id,
                               document.source_content_hash,
                               document.source_artifact_hash,
                               document.pdf_artifact_hash,
                               chunk.id AS chunk_id,
                               chunk.raw_content_hash,
                               chunk.embedding_input_hash,
                               chunk.source_start_page,
                               chunk.source_start_block,
                               chunk.source_end_block
                        FROM knowledge_document_version document
                        JOIN knowledge_chunk chunk
                          ON chunk.tenant_id = document.tenant_id
                         AND chunk.document_version_id = document.id
                        WHERE document.tenant_id = :tenantId
                        ORDER BY document.document_id, document.document_version, chunk.chunk_ordinal
                        """)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .query()
                .listOfRows();
    }

    private void insertConflictingVersion(SynTenPdfSourceDocument source) {
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
                            :sourceName, :conflictingHash, 'PDF',
                            :sourceArtifactHash, :pdfArtifactHash,
                            'pdfbox-text-pages/v1', :importedAt
                        )
                        """)
                .param("id", java.util.UUID.fromString("f1111111-1111-4111-8111-111111111111"))
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
                .param("conflictingHash", "f".repeat(64))
                .param("sourceArtifactHash", source.sourceSha256())
                .param("pdfArtifactHash", source.pdfSha256())
                .param("importedAt", utc(java.time.Instant.parse("2026-08-31T18:00:00Z")))
                .update();
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
