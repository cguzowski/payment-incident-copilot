package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeIngestionService;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
import com.cguzowski.paymentcopilot.knowledge.catalog.SynTenPdfCatalogImportService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "app.knowledge.retrieval.pdf-only=true")
@Testcontainers(disabledWithoutDocker = true)
class PaymentPdfCatalogPostgresIntegrationTest {
    static final UUID TENANT = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");

    @Container
    static final org.testcontainers.postgresql.PostgreSQLContainer POSTGRES =
            new org.testcontainers.postgresql.PostgreSQLContainer("pgvector/pgvector:pg17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add(
                "app.knowledge.pdf-catalog.corpus-root",
                () -> Path.of("..", "..", "SynTen Inc", "payment-knowledge", "v1")
                        .toAbsolutePath()
                        .toString());
    }

    @Autowired
    JdbcClient jdbc;

    @Autowired
    SynTenPdfCatalogImportService importer;

    @Autowired
    KnowledgeSearchRepository search;

    @Autowired
    KnowledgeIngestionService markdown;

    @MockitoBean
    KnowledgeEmbeddingClient embeddings;

    @BeforeEach
    void prepare() {
        jdbc.sql("DELETE FROM knowledge_retrieval_result").update();
        jdbc.sql("DELETE FROM knowledge_retrieval_attempt").update();
        jdbc.sql("DELETE FROM knowledge_chunk").update();
        jdbc.sql("DELETE FROM knowledge_document_version").update();
        float[] vector = new float[768];
        vector[0] = 1;
        org.mockito.Mockito.when(embeddings.embed(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new KnowledgeEmbedding("nomic-embed-text", 768, true, vector));
    }

    private KnowledgeSearchRequest request(UUID tenant, String family, Instant at) {
        return new KnowledgeSearchRequest(
                tenant,
                family,
                at,
                "evidence retry human authority refund capture settlement delivery reconciliation authorization",
                "nomic-embed-text",
                768,
                null,
                20,
                60,
                0,
                .55f);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "AUTHORIZATION_TIMEOUT_SPIKE",
                "CAPTURE_FAILURE_SPIKE",
                "REFUND_FAILURE_SPIKE",
                "SETTLEMENT_DELAY",
                "WEBHOOK_DELIVERY_FAILURE",
                "RECONCILIATION_MISMATCH"
            })
    void everyFamilyRetrievesApplicablePdfRunbooksAndSharedPolicies(String family) {
        importer.importCorpus();
        var candidates = search.search(request(TENANT, family, Instant.parse("2026-10-02T00:00:00Z")));
        assertThat(candidates).isNotEmpty().allSatisfy(c -> {
            assertThat(c.sourceFormat()).isEqualTo(KnowledgeSourceFormat.PDF);
            assertThat(c.pdfSha256()).matches("[a-f0-9]{64}");
            assertThat(c.sourceStartPage()).isBetween(1, 3);
            assertThat(c.sourceStartBlock()).isPositive();
            assertThat(c.sourceStartLine()).isNull();
        });
        assertThat(candidates).anyMatch(c -> c.documentType().name().equals("RUNBOOK"));
        assertThat(candidates).anyMatch(c -> c.documentType().name().equals("POLICY"));
        assertThat(candidates).anyMatch(c -> c.rankingEvidence().relationship() != null);
        if (family.equals("AUTHORIZATION_TIMEOUT_SPIKE")) {
            assertThat(candidates)
                    .noneMatch(c -> c.documentTitle().equals("Capture acknowledgement and amount checks"));
        }
    }

    @Test
    void pdfOnlySearchNeverFallsBackToMarkdownOrOtherTenantOrFutureSources() {
        markdown.importApprovedSources();
        var before = jdbc.sql("SELECT id, source_content_hash FROM knowledge_document_version ORDER BY id")
                .query()
                .listOfRows();
        assertThat(search.search(
                        request(TENANT, "AUTHORIZATION_DECLINE_RATE_SPIKE", Instant.parse("2026-10-02T00:00:00Z"))))
                .isEmpty();
        importer.importCorpus();
        assertThat(search.search(
                        request(UUID.randomUUID(), "REFUND_FAILURE_SPIKE", Instant.parse("2026-10-02T00:00:00Z"))))
                .isEmpty();
        assertThat(search.search(request(TENANT, "REFUND_FAILURE_SPIKE", Instant.parse("2026-09-01T00:00:00Z"))))
                .isEmpty();
        assertThat(search.search(request(TENANT, "REFUND_FAILURE_SPIKE", Instant.parse("2026-10-02T00:00:00Z"))))
                .allMatch(c -> c.sourceFormat() == KnowledgeSourceFormat.PDF);
        assertThat(jdbc.sql(
                                "SELECT id, source_content_hash FROM knowledge_document_version WHERE source_format='MARKDOWN' ORDER BY id")
                        .query()
                        .listOfRows())
                .isEqualTo(before);
        jdbc.sql("UPDATE knowledge_document_version SET catalog_version='unaccepted/v9' WHERE source_format='PDF'")
                .update();
        assertThat(search.search(request(TENANT, "REFUND_FAILURE_SPIKE", Instant.parse("2026-10-02T00:00:00Z"))))
                .isEmpty();
        jdbc.sql(
                        "UPDATE knowledge_document_version SET catalog_version='synten-payment-knowledge/v1' WHERE source_format='PDF'")
                .update();
        jdbc.sql("UPDATE knowledge_document_version SET approval_status='SUPERSEDED' WHERE source_format='PDF'")
                .update();
        assertThat(search.search(request(TENANT, "REFUND_FAILURE_SPIKE", Instant.parse("2026-10-02T00:00:00Z"))))
                .isEmpty();
    }
}
