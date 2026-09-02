package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(SynTenPdfEmbeddingPersistencePostgresIntegrationTest.EmbeddingClientConfiguration.class)
class SynTenPdfEmbeddingPersistencePostgresIntegrationTest {

    private static final Path CORPUS_ROOT =
            Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize();
    private static final Instant EMBEDDED_AT = Instant.parse("2026-09-01T09:00:00Z");
    private static final UUID HISTORICAL_DOCUMENT_VERSION_ID = UUID.fromString("91111111-1111-4111-8111-111111111111");
    private static final UUID HISTORICAL_CHUNK_ID = UUID.fromString("92222222-2222-4222-8222-222222222222");

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
    private SynTenPdfCatalogPlanner planner;

    @Autowired
    private SynTenPdfEmbeddingPersistence persistence;

    @Autowired
    private SynTenPdfEmbeddingService embeddingService;

    @Autowired
    private ConcurrentBackfillEmbeddingClient embeddingClient;

    @BeforeEach
    void clearCatalog() {
        jdbcClient
                .sql("DROP TRIGGER IF EXISTS fail_synten_final_embedding ON knowledge_chunk")
                .update();
        jdbcClient.sql("DROP FUNCTION IF EXISTS fail_synten_final_embedding()").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbcClient.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbcClient.sql("DELETE FROM knowledge_chunk").update();
        jdbcClient.sql("DELETE FROM knowledge_document_version").update();
    }

    @Test
    void atomicallyEmbedsAll705RowsWithoutChangingAnySourceColumnOrHistoricalTitanRow() {
        importService.importCorpus();
        insertHistoricalTitanRow();
        List<Map<String, Object>> sourceBefore = sourceSnapshot();
        List<Map<String, Object>> historicalBefore = historicalSnapshot();

        SynTenPdfEmbeddingOperationSummary summary = persistence.persist(prepared(), EMBEDDED_AT);

        assertThat(summary)
                .isEqualTo(new SynTenPdfEmbeddingOperationSummary(
                        SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT,
                        SynTenPdfEmbeddingState.ABSENT,
                        705,
                        0,
                        false));
        assertThat(sourceSnapshot()).isEqualTo(sourceBefore);
        assertThat(historicalSnapshot()).isEqualTo(historicalBefore);
        assertThat(completeCurrentPdfCount()).isEqualTo(705L);
        assertThat(distinctPdfEmbeddingTimestamps()).isEqualTo(1L);
    }

    @Test
    void rollsBackAllEarlierUpdatesWhenTheFinalUpdateFails() {
        importService.importCorpus();
        List<PreparedSynTenPdfEmbedding> prepared = prepared();
        installFailureTrigger(prepared.getLast().target().chunkId());

        assertThatThrownBy(() -> persistence.persist(prepared, EMBEDDED_AT))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("forced final embedding failure");

        assertThat(embeddedPdfCount()).isZero();
    }

    @Test
    void rejectsAnOptimisticHashChangeBeforeTheFirstUpdate() {
        importService.importCorpus();
        List<PreparedSynTenPdfEmbedding> prepared = prepared();
        jdbcClient
                .sql("UPDATE knowledge_chunk SET embedding_input_hash = :hash WHERE id = :chunkId")
                .param("hash", "f".repeat(64))
                .param("chunkId", prepared.get(352).target().chunkId())
                .update();

        assertThatThrownBy(() -> persistence.persist(prepared, EMBEDDED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("changed after embedding preparation");

        assertThat(embeddedPdfCount()).isZero();
    }

    @Test
    void repeatsAsAnExactNoOpWithoutChangingVectorsOrTimestamps() {
        importService.importCorpus();
        List<PreparedSynTenPdfEmbedding> prepared = prepared();
        persistence.persist(prepared, EMBEDDED_AT);
        List<Map<String, Object>> embeddingBefore = embeddingSnapshot();

        SynTenPdfEmbeddingOperationSummary repeat = persistence.persist(prepared, EMBEDDED_AT.plusSeconds(60));

        assertThat(repeat)
                .isEqualTo(new SynTenPdfEmbeddingOperationSummary(
                        SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT,
                        SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL,
                        705,
                        705,
                        true));
        assertThat(embeddingSnapshot()).isEqualTo(embeddingBefore);
    }

    @Test
    void serializesTwoBackfillsFromTheSameAbsentSnapshotIntoOneWriteAndOneNoOp()
            throws InterruptedException, ExecutionException, TimeoutException {
        importService.importCorpus();
        String finalInput = planner.plan().embeddingTargets().getLast().embeddingInput();
        assertThat(planner.plan().embeddingTargets())
                .filteredOn(target -> target.embeddingInput().equals(finalInput))
                .hasSize(1);
        embeddingClient.prepare(finalInput);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SynTenPdfEmbeddingOperationSummary> first =
                    executor.submit(() -> runBackfillWhenReleased(ready, start));
            Future<SynTenPdfEmbeddingOperationSummary> second =
                    executor.submit(() -> runBackfillWhenReleased(ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(embeddingClient.awaitBothPrepared()).isTrue();
            embeddingClient.releasePersistence();

            List<SynTenPdfEmbeddingOperationSummary> summaries =
                    List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

            assertThat(summaries)
                    .extracting(SynTenPdfEmbeddingOperationSummary::initialState)
                    .containsExactlyInAnyOrder(
                            SynTenPdfEmbeddingState.ABSENT, SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL);
            assertThat(summaries)
                    .filteredOn(SynTenPdfEmbeddingOperationSummary::noOp)
                    .hasSize(1);
            assertThat(summaries).filteredOn(summary -> !summary.noOp()).hasSize(1);
            assertThat(embeddingClient.calls()).isEqualTo(1_410);
            assertThat(embeddedPdfCount()).isEqualTo(705L);
            assertThat(distinctPdfEmbeddingTimestamps()).isEqualTo(1L);
        } finally {
            embeddingClient.releasePersistence();
            executor.shutdownNow();
        }
    }

    private SynTenPdfEmbeddingOperationSummary runBackfillWhenReleased(CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent backfill start barrier timed out.");
        }
        return embeddingService.backfill();
    }

    private List<PreparedSynTenPdfEmbedding> prepared() {
        float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
        vector[0] = 1.0f;
        return planner.plan().embeddingTargets().stream()
                .map(target -> new PreparedSynTenPdfEmbedding(
                        target, KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector))
                .toList();
    }

    private List<Map<String, Object>> sourceSnapshot() {
        return jdbcClient
                .sql("""
                        SELECT document.id AS document_version_id,
                               document.tenant_id,
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
                               document.imported_at,
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
                               chunk.estimated_tokens
                        FROM knowledge_document_version document
                        JOIN knowledge_chunk chunk
                          ON chunk.tenant_id = document.tenant_id
                         AND chunk.document_version_id = document.id
                        WHERE document.tenant_id = :tenantId
                        ORDER BY document.id, chunk.id
                        """)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .query()
                .listOfRows();
    }

    private List<Map<String, Object>> embeddingSnapshot() {
        return jdbcClient
                .sql("""
                        SELECT chunk.id,
                               chunk.embedding_model_id,
                               chunk.embedding_dimensions,
                               chunk.embedding_normalized,
                               chunk.embedded_at,
                               chunk.embedding::text AS embedding
                        FROM knowledge_chunk chunk
                        JOIN knowledge_document_version document
                          ON document.tenant_id = chunk.tenant_id
                         AND document.id = chunk.document_version_id
                        WHERE document.tenant_id = :tenantId
                          AND document.source_format = 'PDF'
                        ORDER BY chunk.id
                        """)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .query()
                .listOfRows();
    }

    private List<Map<String, Object>> historicalSnapshot() {
        return jdbcClient.sql("""
                        SELECT chunk.id,
                               chunk.embedding_model_id,
                               chunk.embedding_dimensions,
                               chunk.embedding_normalized,
                               chunk.embedded_at,
                               chunk.embedding::text AS embedding
                        FROM knowledge_chunk chunk
                        WHERE chunk.id = :chunkId
                        """).param("chunkId", HISTORICAL_CHUNK_ID).query().listOfRows();
    }

    private long completeCurrentPdfCount() {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM knowledge_chunk chunk
                        JOIN knowledge_document_version document
                          ON document.tenant_id = chunk.tenant_id
                         AND document.id = chunk.document_version_id
                        WHERE document.tenant_id = :tenantId
                          AND document.source_format = 'PDF'
                          AND chunk.embedding_model_id = :modelId
                          AND chunk.embedding_dimensions = :dimensions
                          AND chunk.embedding_normalized
                          AND chunk.embedded_at = :embeddedAt
                          AND chunk.embedding IS NOT NULL
                          AND vector_dims(chunk.embedding) = :dimensions
                          AND ABS(vector_norm(chunk.embedding) - 1.0) <= 0.01
                        """)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .param("modelId", KnowledgeEmbeddingClient.MODEL_ID)
                .param("dimensions", KnowledgeEmbeddingClient.DIMENSIONS)
                .param("embeddedAt", java.time.OffsetDateTime.ofInstant(EMBEDDED_AT, java.time.ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    private long distinctPdfEmbeddingTimestamps() {
        return jdbcClient
                .sql("""
                        SELECT COUNT(DISTINCT chunk.embedded_at)
                        FROM knowledge_chunk chunk
                        JOIN knowledge_document_version document
                          ON document.tenant_id = chunk.tenant_id
                         AND document.id = chunk.document_version_id
                        WHERE document.tenant_id = :tenantId
                          AND document.source_format = 'PDF'
                        """)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .query(Long.class)
                .single();
    }

    private long embeddedPdfCount() {
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

    private void installFailureTrigger(UUID chunkId) {
        jdbcClient.sql("""
                        CREATE FUNCTION fail_synten_final_embedding()
                        RETURNS trigger
                        LANGUAGE plpgsql
                        AS $$
                        BEGIN
                            RAISE EXCEPTION 'forced final embedding failure';
                        END;
                        $$
                        """).update();
        jdbcClient.sql(("""
                        CREATE TRIGGER fail_synten_final_embedding
                        BEFORE UPDATE OF embedding ON knowledge_chunk
                        FOR EACH ROW
                        WHEN (OLD.id = UUID '%s')
                        EXECUTE FUNCTION fail_synten_final_embedding()
                        """).formatted(chunkId)).update();
    }

    private void insertHistoricalTitanRow() {
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
                            :id, :tenantId, :documentId, 'RUNBOOK', 'Historical fixture',
                            '1.0.0', 'AUTHORIZATION_DECLINE_RATE_SPIKE', 'Card authorization',
                            'APPROVED', :approvedBy,
                            TIMESTAMPTZ '2026-08-20 10:00:00Z',
                            TIMESTAMPTZ '2026-08-21 00:00:00Z',
                            'historical.md', :hash, 'MARKDOWN', :hash, NULL,
                            'markdown-front-matter/v1', TIMESTAMPTZ '2026-08-28 09:00:00Z'
                        )
                        """)
                .param("id", HISTORICAL_DOCUMENT_VERSION_ID)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .param("documentId", UUID.fromString("93333333-3333-4333-8333-333333333333"))
                .param("approvedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("hash", "a".repeat(64))
                .update();
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
                            :id, :tenantId, :documentVersionId, 0,
                            'Historical', 'Synthetic historical source.',
                            'Document: Historical fixture', :rawHash, :embeddingHash,
                            'embedding-input/v1', 'markdown-sections/v1', 1, 1, 4,
                            'amazon.titan-embed-text-v2:0', 1024, TRUE,
                            TIMESTAMPTZ '2026-08-28 09:00:00Z', CAST(:embedding AS vector)
                        )
                        """)
                .param("id", HISTORICAL_CHUNK_ID)
                .param("tenantId", SynTenCorpusSourceRepository.SYNTEN_TENANT_ID)
                .param("documentVersionId", HISTORICAL_DOCUMENT_VERSION_ID)
                .param("rawHash", "b".repeat(64))
                .param("embeddingHash", "c".repeat(64))
                .param("embedding", unitVectorLiteral(1024))
                .update();
    }

    private static String unitVectorLiteral(int dimensions) {
        return "[1" + ",0".repeat(dimensions - 1) + "]";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmbeddingClientConfiguration {

        @Bean
        @Primary
        ConcurrentBackfillEmbeddingClient concurrentBackfillEmbeddingClient() {
            return new ConcurrentBackfillEmbeddingClient();
        }
    }

    static final class ConcurrentBackfillEmbeddingClient implements KnowledgeEmbeddingClient {

        private final AtomicInteger calls = new AtomicInteger();
        private volatile String finalInput;
        private volatile CountDownLatch bothPrepared = new CountDownLatch(0);
        private volatile CountDownLatch persist = new CountDownLatch(0);

        void prepare(String finalInput) {
            this.finalInput = finalInput;
            calls.set(0);
            bothPrepared = new CountDownLatch(2);
            persist = new CountDownLatch(1);
        }

        @Override
        public KnowledgeEmbedding embed(String input) {
            calls.incrementAndGet();
            if (input.equals(finalInput)) {
                bothPrepared.countDown();
                try {
                    if (!persist.await(20, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent persistence barrier timed out.");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new KnowledgeEmbeddingUnavailableException(exception);
                }
            }
            float[] vector = new float[KnowledgeEmbeddingClient.DIMENSIONS];
            vector[0] = 1.0f;
            return new KnowledgeEmbedding(
                    KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, true, vector);
        }

        boolean awaitBothPrepared() throws InterruptedException {
            return bothPrepared.await(20, TimeUnit.SECONDS);
        }

        void releasePersistence() {
            persist.countDown();
        }

        int calls() {
            return calls.get();
        }
    }
}
