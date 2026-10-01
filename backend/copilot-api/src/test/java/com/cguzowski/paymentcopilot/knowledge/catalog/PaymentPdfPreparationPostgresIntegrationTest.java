package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentPdfPreparationPostgresIntegrationTest {

    @Test
    void retainsUntaggedHistoricalPdfVersionsOutsideTheActiveEmbeddingPlan() {
        importer.importCorpus();
        jdbc.sql("""
                INSERT INTO knowledge_document_version (
                    id, tenant_id, document_id, document_type, title, document_version,
                    incident_family, applies_to, approval_status, approved_by, approved_at,
                    effective_at, source_name, source_content_hash, source_format,
                    source_artifact_hash, pdf_artifact_hash, extraction_strategy_version, imported_at)
                SELECT gen_random_uuid(), tenant_id, document_id, document_type, title, '0.9.0',
                    incident_family, applies_to, approval_status, approved_by, approved_at,
                    effective_at, source_name, source_content_hash, source_format,
                    source_artifact_hash, pdf_artifact_hash, extraction_strategy_version, imported_at
                FROM knowledge_document_version ORDER BY id LIMIT 1
                """).update();
        var before = jdbc.sql(
                        "SELECT to_jsonb(d)::text FROM knowledge_document_version d WHERE document_version='0.9.0'")
                .query(String.class)
                .list();
        assertThat(plans.planBackfill().targets()).hasSize(65);
        assertThat(jdbc.sql("SELECT to_jsonb(d)::text FROM knowledge_document_version d WHERE document_version='0.9.0'")
                        .query(String.class)
                        .list())
                .isEqualTo(before);
    }

    @Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
            new org.testcontainers.postgresql.PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("app.knowledge.pdf-catalog.corpus-root", PaymentPdfCatalogTest.ROOT::toString);
    }

    @Autowired
    JdbcClient jdbc;

    @Autowired
    SynTenPdfCatalogImportService importer;

    @Autowired
    SynTenPdfCatalogPersistenceService persistence;

    @Autowired
    SynTenPdfCatalogSnapshotRepository snapshots;

    @Autowired
    SynTenPdfEmbeddingService embeddingService;

    @Autowired
    SynTenPdfEmbeddingPlanService plans;

    @MockitoBean
    KnowledgeEmbeddingClient embeddings;

    @BeforeEach
    void clear() {
        jdbc.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbc.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbc.sql("DELETE FROM knowledge_chunk").update();
        jdbc.sql("DELETE FROM knowledge_document_version").update();
        org.mockito.Mockito.reset(embeddings);
        float[] vector = new float[768];
        vector[0] = 1;
        org.mockito.Mockito.when(embeddings.embed(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new KnowledgeEmbedding("nomic-embed-text", 768, true, vector));
    }

    @Test
    void catalogsCoexistWithIsolatedEmbeddingPlansAndRepeatablePreparation() {
        var historical = PaymentPdfCatalogTest.planner(Path.of("..", "..", "SynTen Inc", "corpus"))
                .plan();
        persistence.importAll(historical, Instant.now());
        assertThat(importer.importCorpus()).isEqualTo(new PdfCatalogImportSummary(16, 0, 65));
        assertThat(importer.importCorpus()).isEqualTo(new PdfCatalogImportSummary(0, 16, 0));
        var snapshot = snapshots.readEmbeddingSnapshot(
                PaymentPdfCatalogTest.planner(PaymentPdfCatalogTest.ROOT).plan());
        assertThat(snapshot.targets()).hasSize(65);
        assertThat(snapshot.state()).isEqualTo(SynTenPdfEmbeddingState.ABSENT);
        embeddingService.backfill();
        assertThat(embeddingService.backfill().noOp()).isTrue();
        org.mockito.Mockito.verify(embeddings, org.mockito.Mockito.times(65))
                .embed(org.mockito.ArgumentMatchers.anyString());
        assertThat(snapshots.readEmbeddingSnapshot(historical).state()).isEqualTo(SynTenPdfEmbeddingState.ABSENT);
    }

    @Test
    void failedEmbeddingPublishesNoPartialVectors() {
        importer.importCorpus();
        assertThatThrownBy(() -> new SynTenPdfReadinessCommand(plans).run(null))
                .hasMessageContaining("PrepareKnowledge");
        org.mockito.Mockito.when(embeddings.embed(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new IllegalStateException("test unavailable"));
        assertThatThrownBy(embeddingService::backfill).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM knowledge_chunk WHERE embedding IS NOT NULL")
                        .query(Long.class)
                        .single())
                .isZero();
        assertThatThrownBy(() -> new SynTenPdfReadinessCommand(plans).run(null))
                .hasMessageContaining("PrepareKnowledge");
    }

    @Test
    void readinessRejectsChangedApplicabilityInsteadOfSilentlyRepairingIt() {
        importer.importCorpus();
        jdbc.sql(
                        "UPDATE knowledge_document_version SET applicable_families=ARRAY['REFUND_FAILURE_SPIKE'] WHERE document_key='RB-101'")
                .update();
        assertThatThrownBy(plans::planBackfill).hasMessageContaining("applicability");
        assertThatThrownBy(importer::importCorpus).hasMessageContaining("relationships");
    }

    @Test
    void conflictingLastVersionRollsBackEntireSuccessorImport() {
        importer.importCorpus();
        jdbc.sql(
                        "DELETE FROM knowledge_chunk WHERE document_version_id IN (SELECT id FROM knowledge_document_version WHERE document_key<>'PL-105')")
                .update();
        jdbc.sql("DELETE FROM knowledge_document_version WHERE document_key<>'PL-105'")
                .update();
        jdbc.sql("UPDATE knowledge_document_version SET source_content_hash=:hash")
                .param("hash", "0".repeat(64))
                .update();
        assertThatThrownBy(importer::importCorpus).hasMessageContaining("requires a new document version");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM knowledge_document_version")
                        .query(Long.class)
                        .single())
                .isEqualTo(1);
    }
}
