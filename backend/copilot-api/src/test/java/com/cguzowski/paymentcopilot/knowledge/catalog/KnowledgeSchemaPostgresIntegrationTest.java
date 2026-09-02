package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
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
class KnowledgeSchemaPostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID DOCUMENT_ID = UUID.fromString("66a84fed-3d77-4e7e-9a1b-e25ff37e2280");
    private static final UUID DOCUMENT_VERSION_ID = UUID.fromString("a37cd2fa-a938-4e4e-b7f3-956f8d293f28");
    private static final UUID CHUNK_ID = UUID.fromString("b0f6a021-e6d3-4f05-bbad-7a2cb7652714");
    private static final UUID HISTORICAL_CHUNK_ID = UUID.fromString("c0f6a021-e6d3-4f05-bbad-7a2cb7652714");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("payment_copilot")
            .withUsername("payment_copilot")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanKnowledge() {
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
    }

    @Test
    void preservesPreviouslyAppliedReportMigrationBeforeOllamaMigration() {
        assertThat(jdbcClient.sql("""
                                SELECT version, description, checksum
                                FROM flyway_schema_history
                                WHERE version IN ('6', '7')
                                ORDER BY installed_rank
                                """).query().listOfRows())
                .containsExactly(
                        Map.of(
                                "version",
                                "6",
                                "description",
                                "add evidence linked report generation",
                                "checksum",
                                -54318256),
                        Map.of(
                                "version",
                                "7",
                                "description",
                                "support local ollama embeddings",
                                "checksum",
                                1491731562));
    }

    @Test
    void storesNomicEmbeddingAndPreservesHistoricalTitanContract() {
        insertDocument();
        insertChunk(TENANT_ID, CHUNK_ID, 0, "nomic-embed-text", 768, vector(768));
        insertChunk(TENANT_ID, HISTORICAL_CHUNK_ID, 1, "amazon.titan-embed-text-v2:0", 1024, vector(1024));

        Map<String, Object> stored =
                jdbcClient.sql("""
                        SELECT raw_content, embedding_input, embedding_model_id,
                               embedding_dimensions, embedding_normalized,
                               vector_dims(embedding) AS dimensions
                        FROM knowledge_chunk
                        WHERE id = :id
                        """).param("id", CHUNK_ID).query().singleRow();
        assertThat(stored.get("raw_content")).isEqualTo("Exact source paragraph.");
        assertThat(stored.get("embedding_input")).isEqualTo("""
                Document: Authorization Decline Runbook
                Section: Gateway Failures > Diagnosis
                Type: RUNBOOK
                Applies to: Card authorization

                Exact source paragraph.""");
        assertThat(stored.get("embedding_model_id")).isEqualTo("nomic-embed-text");
        assertThat(stored.get("embedding_dimensions")).isEqualTo(768);
        assertThat(stored.get("embedding_normalized")).isEqualTo(true);
        assertThat(stored.get("dimensions")).isEqualTo(768);
        assertThat(jdbcClient
                        .sql("SELECT vector_dims(embedding) FROM knowledge_chunk WHERE id = :id")
                        .param("id", HISTORICAL_CHUNK_ID)
                        .query(Integer.class)
                        .single())
                .isEqualTo(1024);

        assertThatThrownBy(() -> insertChunk(
                        OTHER_TENANT_ID,
                        UUID.fromString("d0f6a021-e6d3-4f05-bbad-7a2cb7652714"),
                        2,
                        "nomic-embed-text",
                        768,
                        vector(768)))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertChunk(
                        TENANT_ID,
                        UUID.fromString("e0f6a021-e6d3-4f05-bbad-7a2cb7652714"),
                        2,
                        "nomic-embed-text",
                        768,
                        vector(512)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void storesPdfLocatorWithoutEmbeddingAndRejectsIncompleteCatalogRows() {
        assertThat(jdbcClient
                        .sql("SELECT description FROM flyway_schema_history WHERE version = '9'")
                        .query(String.class)
                        .single())
                .isEqualTo("add page aware pdf knowledge catalog");
        insertPdfDocument();
        insertPdfChunk(CHUNK_ID, false, false);

        assertThat(jdbcClient.sql("""
                                SELECT d.source_format, d.source_artifact_hash,
                                       d.pdf_artifact_hash, d.extraction_strategy_version,
                                       c.source_start_line, c.source_start_page,
                                       c.source_end_page, c.source_start_block,
                                       c.source_end_block, c.embedding_model_id,
                                       c.embedding_dimensions, c.embedding
                                FROM knowledge_chunk c
                                JOIN knowledge_document_version d
                                  ON d.tenant_id = c.tenant_id
                                 AND d.id = c.document_version_id
                                WHERE c.id = :id
                                """).param("id", CHUNK_ID).query().singleRow())
                .containsEntry("source_format", "PDF")
                .containsEntry("source_artifact_hash", "a".repeat(64))
                .containsEntry("pdf_artifact_hash", "d".repeat(64))
                .containsEntry("extraction_strategy_version", "pdfbox-text-pages/v1")
                .containsEntry("source_start_page", 3)
                .containsEntry("source_end_page", 3)
                .containsEntry("source_start_block", 4)
                .containsEntry("source_end_block", 8)
                .containsEntry("source_start_line", null)
                .containsEntry("embedding_model_id", null)
                .containsEntry("embedding_dimensions", null)
                .containsEntry("embedding", null);

        assertThatThrownBy(() -> insertPdfChunk(UUID.fromString("d0f6a021-e6d3-4f05-bbad-7a2cb7652714"), true, false))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertPdfChunk(UUID.fromString("e0f6a021-e6d3-4f05-bbad-7a2cb7652714"), false, true))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void rejectsPdfWithoutArtifactHashAndOutOfRangePageOrTokenCount() {
        assertThatThrownBy(this::insertPdfDocumentWithoutHash).isInstanceOf(RuntimeException.class);
        insertPdfDocument();

        assertThatThrownBy(() ->
                        insertPdfChunk(UUID.fromString("f0f6a021-e6d3-4f05-bbad-7a2cb7652714"), false, false, 16, 5))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() ->
                        insertPdfChunk(UUID.fromString("f1f6a021-e6d3-4f05-bbad-7a2cb7652714"), false, false, 3, 601))
                .isInstanceOf(RuntimeException.class);
    }

    private void insertDocument() {
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
                            :id, :tenantId, :documentId, 'RUNBOOK',
                            'Authorization Decline Runbook', '1.0.0',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'Card authorization',
                            'APPROVED', :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            'authorization-decline-runbook.md', :hash, 'MARKDOWN',
                            :hash, NULL, 'markdown-front-matter/v1',
                            TIMESTAMPTZ '2026-08-28 10:00:00Z'
                        )
                        """)
                .param("id", DOCUMENT_VERSION_ID)
                .param("tenantId", TENANT_ID)
                .param("documentId", DOCUMENT_ID)
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("hash", "a".repeat(64))
                .update();
    }

    private void insertPdfDocument() {
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
                            :id, :tenantId, :documentId, 'RUNBOOK',
                            'Authorization Decline Runbook', '1.0.0',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'Card authorization',
                            'APPROVED', :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            'rb-001.pdf', :contentHash, 'PDF', :sourceHash,
                            :pdfHash, 'pdfbox-text-pages/v1',
                            TIMESTAMPTZ '2026-08-28 10:00:00Z'
                        )
                        """)
                .param("id", DOCUMENT_VERSION_ID)
                .param("tenantId", TENANT_ID)
                .param("documentId", DOCUMENT_ID)
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("contentHash", "e".repeat(64))
                .param("sourceHash", "a".repeat(64))
                .param("pdfHash", "d".repeat(64))
                .update();
    }

    private void insertPdfDocumentWithoutHash() {
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
                            :id, :tenantId, :documentId, 'RUNBOOK',
                            'Authorization Decline Runbook', '1.0.0',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'Card authorization',
                            'APPROVED', :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            'rb-001.pdf', :contentHash, 'PDF', :sourceHash,
                            NULL, 'pdfbox-text-pages/v1',
                            TIMESTAMPTZ '2026-08-28 10:00:00Z'
                        )
                        """)
                .param("id", DOCUMENT_VERSION_ID)
                .param("tenantId", TENANT_ID)
                .param("documentId", DOCUMENT_ID)
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("contentHash", "e".repeat(64))
                .param("sourceHash", "a".repeat(64))
                .update();
    }

    private void insertPdfChunk(UUID chunkId, boolean partialEmbedding, boolean includeLineLocator) {
        insertPdfChunk(chunkId, partialEmbedding, includeLineLocator, 3, 5);
    }

    private void insertPdfChunk(
            UUID chunkId, boolean partialEmbedding, boolean includeLineLocator, int pageNumber, int tokens) {
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
                            :id, :tenantId, :documentVersionId, 0,
                            'Diagnostic procedure', 'Exact PDF source paragraph.',
                            'Document: fixture', :rawHash, :embeddingHash,
                            'embedding-input/v1', 'pdf-page-sections/v1',
                            :startLine, :endLine, :pageNumber, :pageNumber, 4, 8, :tokens,
                            :modelId, NULL, NULL, NULL, NULL
                        )
                        """)
                .param("id", chunkId)
                .param("tenantId", TENANT_ID)
                .param("documentVersionId", DOCUMENT_VERSION_ID)
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .param("startLine", includeLineLocator ? 20 : null)
                .param("endLine", includeLineLocator ? 22 : null)
                .param("pageNumber", pageNumber)
                .param("tokens", tokens)
                .param("modelId", partialEmbedding ? "nomic-embed-text" : null)
                .update();
    }

    private void insertChunk(
            UUID tenantId, UUID chunkId, int ordinal, String modelId, int dimensions, String embedding) {
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_chunk (
                            id, tenant_id, document_version_id, chunk_ordinal,
                            section_path, raw_content, embedding_input,
                            raw_content_hash, embedding_input_hash,
                            embedding_input_template_version, chunking_strategy_version,
                            source_start_line, source_end_line, estimated_tokens,
                            embedding_model_id, embedding_dimensions,
                            embedding_normalized, embedded_at, embedding
                        ) VALUES (
                            :id, :tenantId, :documentVersionId, :ordinal,
                            'Gateway Failures > Diagnosis', 'Exact source paragraph.',
                            :embeddingInput, :rawHash, :embeddingHash,
                            'embedding-input/v1', 'markdown-sections/v1',
                            20, 20, 5, :modelId,
                            :dimensions, TRUE, TIMESTAMPTZ '2026-08-28 10:00:00Z',
                            CAST(:embedding AS vector)
                        )
                        """)
                .param("id", chunkId)
                .param("tenantId", tenantId)
                .param("documentVersionId", DOCUMENT_VERSION_ID)
                .param("ordinal", ordinal)
                .param("modelId", modelId)
                .param("dimensions", dimensions)
                .param("embeddingInput", """
                        Document: Authorization Decline Runbook
                        Section: Gateway Failures > Diagnosis
                        Type: RUNBOOK
                        Applies to: Card authorization

                        Exact source paragraph.""")
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .param("embedding", embedding)
                .update();
    }

    private static String vector(int dimensions) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < dimensions; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(index == 0 ? "1.0" : "0.0");
        }
        return value.append(']').toString();
    }
}
