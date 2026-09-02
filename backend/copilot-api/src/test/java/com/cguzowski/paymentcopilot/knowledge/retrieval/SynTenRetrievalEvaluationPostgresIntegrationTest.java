package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
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
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"spring.ai.model.chat=none", "spring.ai.model.embedding=none"})
@Testcontainers(disabledWithoutDocker = true)
class SynTenRetrievalEvaluationPostgresIntegrationTest {

    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").toAbsolutePath().normalize();
    private static final Path SYNTEN_ROOT = REPOSITORY_ROOT.resolve("SynTen Inc");
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");
    private static final UUID OPERATOR_ID = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");

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
        registry.add(
                "app.knowledge.retrieval-evaluation.cases-path",
                () -> SYNTEN_ROOT.resolve("evaluation/retrieval-cases.md").toString());
        registry.add(
                "app.knowledge.retrieval-evaluation.scenario-catalog-path",
                () -> REPOSITORY_ROOT
                        .resolve("syntheticIncidentGenerator/src/main/resources/scenarios/catalog.json")
                        .toString());
        registry.add(
                "app.knowledge.retrieval-evaluation.corpus-root",
                () -> SYNTEN_ROOT.resolve("corpus").toString());
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private SynTenRetrievalEvaluationContractRepository contracts;

    @Autowired
    private SynTenRetrievalEvaluationService service;

    @MockitoBean
    private KnowledgeRetrievalExecutor executor;

    @BeforeEach
    void clearPersistedContext() {
        reset(executor);
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
    }

    @Test
    void executesAll37VariantsFromTenantScopedPersistedIncidentAndEvidenceContext() {
        SynTenRetrievalEvaluationContract contract = contracts.load(EVALUATED_AT);
        SynTenRetrievalEvaluationSeed seed = seed(contract);
        Map<UUID, SynTenRetrievalEvaluationSeedMapping> byInvestigation = new LinkedHashMap<>();
        for (SynTenRetrievalEvaluationSeedMapping mapping : seed.mappings()) {
            insertContext(mapping, contract);
            byInvestigation.put(mapping.investigationId(), mapping);
        }
        when(executor.execute(any(), org.mockito.Mockito.eq(EVALUATED_AT))).thenAnswer(invocation -> {
            KnowledgeRetrievalContext context = invocation.getArgument(0);
            return execution(byInvestigation.get(context.investigationId()), context, contract);
        });

        SynTenRetrievalEvaluationRun run = service.evaluate(seed, contract);

        assertThat(run.grade().passed()).isTrue();
        assertThat(run.variants()).hasSize(37);
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM incident")
                        .query(Integer.class)
                        .single())
                .isEqualTo(37);
        assertThat(jdbcClient
                        .sql("SELECT COUNT(*) FROM evidence_collection_attempt")
                        .query(Integer.class)
                        .single())
                .isEqualTo(37);
        verify(executor, org.mockito.Mockito.times(37)).execute(any(), org.mockito.Mockito.eq(EVALUATED_AT));
    }

    private void insertContext(
            SynTenRetrievalEvaluationSeedMapping mapping, SynTenRetrievalEvaluationContract contract) {
        long ordinal = seedOrdinal(mapping);
        UUID correlationId = new UUID(4, ordinal);
        ExpectedScenario expected = expected(mapping, contract);
        jdbcClient
                .sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity,
                            status, summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, :reference, :family, 'HIGH', 'INVESTIGATING',
                            :title, :description, :occurredAt, :receivedAt
                        )
                        """)
                .param("id", mapping.incidentId())
                .param("tenantId", SynTenRetrievalEvaluationContractRepository.TENANT_ID)
                .param("reference", mapping.scenarioReference())
                .param("family", SynTenRetrievalEvaluationContractRepository.INCIDENT_FAMILY)
                .param("title", expected.title())
                .param("description", expected.description())
                .param("occurredAt", OffsetDateTime.ofInstant(EVALUATED_AT.minusSeconds(300), ZoneOffset.UTC))
                .param("receivedAt", OffsetDateTime.ofInstant(EVALUATED_AT.minusSeconds(240), ZoneOffset.UTC))
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO investigation (
                            id, tenant_id, incident_id, started_by, started_at, correlation_id
                        ) VALUES (:id, :tenantId, :incidentId, :operatorId, :startedAt, :correlationId)
                        """)
                .param("id", mapping.investigationId())
                .param("tenantId", SynTenRetrievalEvaluationContractRepository.TENANT_ID)
                .param("incidentId", mapping.incidentId())
                .param("operatorId", OPERATOR_ID)
                .param("startedAt", OffsetDateTime.ofInstant(EVALUATED_AT.minusSeconds(180), ZoneOffset.UTC))
                .param("correlationId", correlationId)
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO evidence_collection_attempt (
                            id, tenant_id, investigation_id, tool_call_id,
                            investigation_correlation_id, source_system, source_tool,
                            scenario_reference, status, requested_at, retrieved_at,
                            completed_at, content_schema_version, content, requested_by
                        ) VALUES (
                            :id, :tenantId, :investigationId, :toolCallId,
                            :correlationId, 'synthetic-observability', 'getRecentServiceErrors',
                            :reference, :status, :requestedAt, :retrievedAt,
                            :completedAt, 'service-errors/v1', CAST(:content AS JSONB), :operatorId
                        )
                        """)
                .param("id", mapping.evidenceId())
                .param("tenantId", SynTenRetrievalEvaluationContractRepository.TENANT_ID)
                .param("investigationId", mapping.investigationId())
                .param("toolCallId", new UUID(5, ordinal))
                .param("correlationId", correlationId)
                .param("reference", mapping.scenarioReference())
                .param("status", mapping.evidenceStatus())
                .param("requestedAt", OffsetDateTime.ofInstant(EVALUATED_AT.minusSeconds(120), ZoneOffset.UTC))
                .param("retrievedAt", OffsetDateTime.ofInstant(EVALUATED_AT.minusSeconds(119), ZoneOffset.UTC))
                .param("completedAt", OffsetDateTime.ofInstant(EVALUATED_AT.minusSeconds(118), ZoneOffset.UTC))
                .param("content", expected.contentJson())
                .param("operatorId", OPERATOR_ID)
                .update();
    }

    private ExpectedScenario expected(
            SynTenRetrievalEvaluationSeedMapping mapping, SynTenRetrievalEvaluationContract contract) {
        if (mapping.variantId().equals("EXCLUSION")) {
            return new ExpectedScenario(
                    "Synthetic legacy knowledge exclusion probe",
                    "Synthetic exclusion probe for legacy gateway recovery, emergency routing, and AI incident automation.",
                    null);
        }
        EvaluationScenario scenario = contract.scenarios().get(mapping.variantId());
        if (!List.of("AVAILABLE", "PARTIAL").contains(mapping.evidenceStatus())) {
            return new ExpectedScenario(scenario.title(), scenario.description(), null);
        }
        List<Map<String, Object>> errors = new ArrayList<>();
        AtomicLong event = new AtomicLong(1);
        for (EvaluationScenarioError error : scenario.evidence().errors()) {
            errors.add(Map.of(
                    "sourceEventId", mapping.variantId() + "-" + event.getAndIncrement(),
                    "observedAt", EVALUATED_AT.minusSeconds(60).toString(),
                    "errorCode", error.errorCode(),
                    "count", error.count()));
        }
        try {
            return new ExpectedScenario(
                    scenario.title(),
                    scenario.description(),
                    jsonMapper.writeValueAsString(Map.of(
                            "serviceName", scenario.evidence().serviceName(),
                            "observedFrom", EVALUATED_AT.minusSeconds(300).toString(),
                            "observedTo", EVALUATED_AT.minusSeconds(60).toString(),
                            "errors", errors)));
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static KnowledgeRetrievalExecution execution(
            SynTenRetrievalEvaluationSeedMapping mapping,
            KnowledgeRetrievalContext context,
            SynTenRetrievalEvaluationContract contract) {
        RetrievalEvaluationCase evaluationCase = contract.caseById(mapping.caseId());
        List<KnowledgeSearchCandidate> candidates = new ArrayList<>();
        candidates.add(candidate(evaluationCase.primaryRunbookKey(), 1, contract));
        candidates.add(candidate(evaluationCase.supportingPolicyKey(), 2, contract));
        if (evaluationCase.weakApprovedMatchKey() != null) {
            candidates.add(candidate(evaluationCase.weakApprovedMatchKey(), 3, contract));
        }
        return new KnowledgeRetrievalExecution(
                new DerivedKnowledgeQuery(
                        "Observed evidence status: " + context.evidence().latestStatus(),
                        "knowledge-query/v1",
                        List.of(mapping.evidenceId())),
                new KnowledgeMetadataFilters(
                        SynTenRetrievalEvaluationContractRepository.INCIDENT_FAMILY,
                        List.of(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY),
                        KnowledgeApprovalStatus.APPROVED,
                        EVALUATED_AT),
                "postgres-hybrid-rrf/v1",
                60,
                20,
                0.0f,
                0.55f,
                new QueryEmbeddingOutcome(
                        QueryEmbeddingStatus.AVAILABLE,
                        KnowledgeEmbeddingClient.MODEL_ID,
                        KnowledgeEmbeddingClient.DIMENSIONS,
                        true),
                candidates,
                List.of(
                        new SelectedKnowledgeChunk(candidates.get(0), 1, 1),
                        new SelectedKnowledgeChunk(candidates.get(1), 2, 2)),
                KnowledgeRetrievalStatus.AVAILABLE,
                null);
    }

    private static KnowledgeSearchCandidate candidate(
            String key, int rank, SynTenRetrievalEvaluationContract contract) {
        EvaluationDocument document = contract.documents().get(key);
        return new KnowledgeSearchCandidate(
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                UUID.nameUUIDFromBytes((key + "-chunk").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                rank,
                UUID.nameUUIDFromBytes((key + "-version").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                document.documentId(),
                KnowledgeDocumentType.valueOf(document.type()),
                key,
                document.version(),
                document.incidentFamily(),
                "payment authorization",
                "section",
                "raw PDF text excluded from evaluation output",
                Path.of(document.pdf()).getFileName().toString(),
                KnowledgeSourceFormat.PDF,
                document.pdfSha256(),
                null,
                null,
                2,
                2,
                1,
                3,
                KnowledgeApprovalStatus.APPROVED,
                OPERATOR_ID,
                document.approvedAt(),
                document.effectiveAt(),
                0.5f,
                rank,
                0.9f,
                rank,
                2.0d / (60 + rank));
    }

    private static SynTenRetrievalEvaluationSeed seed(SynTenRetrievalEvaluationContract contract) {
        AtomicLong ids = new AtomicLong(1);
        List<SynTenRetrievalEvaluationSeedMapping> mappings = new ArrayList<>();
        for (RetrievalEvaluationCase evaluationCase : contract.cases()) {
            List<String> variants =
                    evaluationCase.caseId().equals("KQ-023") ? List.of("EXCLUSION") : evaluationCase.scenarioIds();
            for (String variant : variants) {
                long id = ids.getAndIncrement();
                String scenario = variant.equals("EXCLUSION") ? "S999" : variant;
                mappings.add(new SynTenRetrievalEvaluationSeedMapping(
                        evaluationCase.caseId(),
                        variant,
                        "sig-v1-" + scenario + "-1788264000-0123456789ab",
                        new UUID(1, id),
                        new UUID(2, id),
                        new UUID(3, id),
                        evidenceStatus(evaluationCase.caseId())));
            }
        }
        return new SynTenRetrievalEvaluationSeed(
                SynTenRetrievalEvaluationSeed.SCHEMA_VERSION,
                contract.evaluationVersion(),
                contract.corpusVersion(),
                "0123456789abcdef0123456789abcdef",
                EVALUATED_AT.plusSeconds(60),
                EVALUATED_AT,
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                "payment_copilot_k4_eval_test",
                mappings);
    }

    private static long seedOrdinal(SynTenRetrievalEvaluationSeedMapping mapping) {
        return mapping.incidentId().getLeastSignificantBits();
    }

    private static String evidenceStatus(String caseId) {
        return switch (caseId) {
            case "KQ-020" -> "PARTIAL";
            case "KQ-022" -> "UNAVAILABLE";
            case "KQ-023" -> "NOT_FOUND";
            default -> "AVAILABLE";
        };
    }

    private record ExpectedScenario(String title, String description, String contentJson) {}
}
