package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
class SynTenPdfEmbeddingPostgresIntegrationTest {

    private static final Path CORPUS_ROOT =
            Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize();
    private static final Instant EMBEDDED_AT = Instant.parse("2026-08-31T20:00:00Z");

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
    private SynTenPdfEmbeddingPlanService planService;

    @BeforeEach
    void clearCatalog() {
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
    }

    @Test
    void readsTheExactAllAbsentCatalogAsStableTargets() {
        assertThat(importService.importCorpus().cataloguedChunks()).isEqualTo(705);

        SynTenPdfEmbeddingCatalogSnapshot snapshot = planService.planBackfill();

        assertThat(snapshot.catalogFingerprint()).isEqualTo(SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT);
        assertThat(snapshot.state()).isEqualTo(SynTenPdfEmbeddingState.ABSENT);
        assertThat(snapshot.targets()).hasSize(705);
        assertThat(snapshot.targets())
                .extracting(SynTenPdfEmbeddingTarget::chunkId)
                .doesNotHaveDuplicates();
        assertThat(snapshot.targets())
                .isSortedAccordingTo(java.util.Comparator.comparing(SynTenPdfEmbeddingTarget::documentKey)
                        .thenComparing(SynTenPdfEmbeddingTarget::documentVersion)
                        .thenComparingInt(SynTenPdfEmbeddingTarget::chunkOrdinal));
        assertThat(embeddedCount()).isZero();
    }

    @Test
    void rejectsMixedCoverageWithoutChangingThePersistedState() {
        importService.importCorpus();
        embedFirstChunk(KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS);

        assertThatThrownBy(planService::planBackfill)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MIXED");
        assertThat(embeddedCount()).isEqualTo(1L);
    }

    @Test
    void rejectsConflictingModelMetadataWithoutChangingThePersistedState() {
        importService.importCorpus();
        embedFirstChunk("amazon.titan-embed-text-v2:0", 1024);

        assertThatThrownBy(planService::planBackfill)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CONFLICTING");
        assertThat(embeddedCount()).isEqualTo(1L);
    }

    @Test
    void treatsACompleteCurrentModelCatalogAsAnExactNoOp() {
        importService.importCorpus();
        embedAllPdfChunks();

        SynTenPdfEmbeddingCatalogSnapshot snapshot = planService.planBackfill();

        assertThat(snapshot.state()).isEqualTo(SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL);
        assertThat(snapshot.embeddedAt()).isEqualTo(EMBEDDED_AT);
        assertThat(snapshot.operationSummary())
                .isEqualTo(new SynTenPdfEmbeddingOperationSummary(
                        SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT,
                        SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL,
                        705,
                        705,
                        true));
        assertThat(embeddedCount()).isEqualTo(705L);
    }

    @Test
    void rejectsPersistedCatalogDriftBeforeAnyEmbeddingWork() {
        importService.importCorpus();
        jdbcClient
                .sql("""
                        UPDATE knowledge_chunk
                        SET raw_content_hash = :changedHash
                        WHERE id = (
                            SELECT chunk.id
                            FROM knowledge_chunk chunk
                            JOIN knowledge_document_version document
                              ON document.tenant_id = chunk.tenant_id
                             AND document.id = chunk.document_version_id
                            WHERE document.tenant_id = :tenantId
                              AND document.source_format = 'PDF'
                            ORDER BY document.document_id, document.document_version, chunk.chunk_ordinal
                            LIMIT 1
                        )
                        """)
                .param("changedHash", "f".repeat(64))
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .update();

        assertThatThrownBy(planService::planBackfill)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("differs from the accepted K3 plan");
        assertThat(embeddedCount()).isZero();
    }

    private void embedFirstChunk(String modelId, int dimensions) {
        jdbcClient
                .sql("""
                        UPDATE knowledge_chunk
                        SET embedding_model_id = :modelId,
                            embedding_dimensions = :dimensions,
                            embedding_normalized = TRUE,
                            embedded_at = :embeddedAt,
                            embedding = CAST(:embedding AS vector)
                        WHERE id = (
                            SELECT chunk.id
                            FROM knowledge_chunk chunk
                            JOIN knowledge_document_version document
                              ON document.tenant_id = chunk.tenant_id
                             AND document.id = chunk.document_version_id
                            WHERE document.tenant_id = :tenantId
                              AND document.source_format = 'PDF'
                            ORDER BY document.document_id, document.document_version, chunk.chunk_ordinal
                            LIMIT 1
                        )
                        """)
                .param("modelId", modelId)
                .param("dimensions", dimensions)
                .param("embeddedAt", utc(EMBEDDED_AT))
                .param("embedding", unitVector(dimensions))
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .update();
    }

    private void embedAllPdfChunks() {
        jdbcClient
                .sql("""
                        UPDATE knowledge_chunk chunk
                        SET embedding_model_id = :modelId,
                            embedding_dimensions = :dimensions,
                            embedding_normalized = TRUE,
                            embedded_at = :embeddedAt,
                            embedding = CAST(:embedding AS vector)
                        FROM knowledge_document_version document
                        WHERE document.tenant_id = chunk.tenant_id
                          AND document.id = chunk.document_version_id
                          AND document.tenant_id = :tenantId
                          AND document.source_format = 'PDF'
                        """)
                .param("modelId", KnowledgeEmbeddingClient.MODEL_ID)
                .param("dimensions", KnowledgeEmbeddingClient.DIMENSIONS)
                .param("embeddedAt", utc(EMBEDDED_AT))
                .param("embedding", unitVector(KnowledgeEmbeddingClient.DIMENSIONS))
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .update();
    }

    private long embeddedCount() {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM knowledge_chunk chunk
                        JOIN knowledge_document_version document
                          ON document.tenant_id = chunk.tenant_id
                         AND document.id = chunk.document_version_id
                        WHERE document.tenant_id = :tenantId
                          AND document.source_format = 'PDF'
                          AND chunk.embedding IS NOT NULL
                        """)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .query(Long.class)
                .single();
    }

    private static String unitVector(int dimensions) {
        return "[1" + ",0".repeat(dimensions - 1) + "]";
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
