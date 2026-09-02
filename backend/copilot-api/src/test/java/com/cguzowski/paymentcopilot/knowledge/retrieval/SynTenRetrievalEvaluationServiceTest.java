package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SynTenRetrievalEvaluationServiceTest {

    private static final Path SYNTEN_ROOT =
            Path.of("..", "..", "SynTen Inc").toAbsolutePath().normalize();
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");
    private static final SynTenRetrievalEvaluationContract CONTRACT = contract();

    @Test
    void executesAll37VariantsThroughTheSharedExecutorAndResolvesManifestProvenance() {
        SynTenRetrievalEvaluationSeed seed = seed();
        KnowledgeRetrievalContextAssembler contexts = mock(KnowledgeRetrievalContextAssembler.class);
        KnowledgeRetrievalExecutor executor = mock(KnowledgeRetrievalExecutor.class);
        for (SynTenRetrievalEvaluationSeedMapping mapping : seed.mappings()) {
            KnowledgeRetrievalContext context = context(mapping);
            when(contexts.find(seed.tenantId(), mapping.investigationId())).thenReturn(Optional.of(context));
            when(executor.execute(context, EVALUATED_AT)).thenReturn(execution(mapping, context));
        }

        SynTenRetrievalEvaluationRun run = new SynTenRetrievalEvaluationService(
                        contexts, executor, new SynTenRetrievalEvaluationGrader())
                .evaluate(seed, CONTRACT);

        assertThat(run.schemaVersion()).isEqualTo("synten-retrieval-eval-result/v1");
        assertThat(run.variants()).hasSize(37);
        assertThat(run.grade().passed()).isTrue();
        assertThat(run.variants()).allSatisfy(variant -> {
            assertThat(variant.result().candidates()).isNotEmpty().allMatch(EvaluationCandidateResult::eligible);
            assertThat(variant.result().candidates())
                    .allMatch(candidate -> CONTRACT.documents().containsKey(candidate.documentKey()));
            assertThat(variant.filters().tenantId()).isEqualTo(seed.tenantId());
            assertThat(variant.filters().approvalStatus()).isEqualTo("APPROVED");
        });
        assertThat(run.variants().getFirst().result().candidates().getFirst().selectedPosition())
                .isEqualTo(1);
        assertThat(run.variants().getFirst().result().candidates().getFirst().sourceStartPage())
                .isEqualTo(2);
        verify(executor, org.mockito.Mockito.times(37)).execute(any(), org.mockito.Mockito.eq(EVALUATED_AT));
    }

    @Test
    void abortsBeforeRetrievalWhenPersistedContextDiffersFromTheReviewedScenario() {
        SynTenRetrievalEvaluationSeed seed = seed();
        SynTenRetrievalEvaluationSeedMapping first = seed.mappings().getFirst();
        KnowledgeRetrievalContextAssembler contexts = mock(KnowledgeRetrievalContextAssembler.class);
        KnowledgeRetrievalExecutor executor = mock(KnowledgeRetrievalExecutor.class);
        KnowledgeRetrievalContext correct = context(first);
        KnowledgeRetrievalContext drifted = new KnowledgeRetrievalContext(
                correct.tenantId(),
                correct.investigationId(),
                correct.investigationCorrelationId(),
                correct.incidentFamily(),
                "drifted title",
                correct.incidentDescription(),
                correct.evidence());
        when(contexts.find(seed.tenantId(), first.investigationId())).thenReturn(Optional.of(drifted));

        assertThatThrownBy(() -> new SynTenRetrievalEvaluationService(
                                contexts, executor, new SynTenRetrievalEvaluationGrader())
                        .evaluate(seed, CONTRACT))
                .hasMessageContaining("persisted context")
                .hasMessageContaining("KQ-001/S001");
        verify(executor, never()).execute(any(), any());
    }

    private static KnowledgeRetrievalExecution execution(
            SynTenRetrievalEvaluationSeedMapping mapping, KnowledgeRetrievalContext context) {
        RetrievalEvaluationCase evaluationCase = CONTRACT.caseById(mapping.caseId());
        List<KnowledgeSearchCandidate> candidates = new ArrayList<>();
        candidates.add(candidate(evaluationCase.primaryRunbookKey(), 1));
        candidates.add(candidate(evaluationCase.supportingPolicyKey(), 2));
        if (evaluationCase.weakApprovedMatchKey() != null) {
            candidates.add(candidate(evaluationCase.weakApprovedMatchKey(), 3));
        }
        List<SelectedKnowledgeChunk> selected = List.of(
                new SelectedKnowledgeChunk(candidates.get(0), 1, 1),
                new SelectedKnowledgeChunk(candidates.get(1), 2, 2));
        String evidenceStatus = context.evidence().latestStatus();
        return new KnowledgeRetrievalExecution(
                new DerivedKnowledgeQuery(
                        "Observed evidence status: " + evidenceStatus,
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
                selected,
                KnowledgeRetrievalStatus.AVAILABLE,
                null);
    }

    private static KnowledgeSearchCandidate candidate(String key, int rank) {
        EvaluationDocument document = CONTRACT.documents().get(key);
        String fileName = Path.of(document.pdf()).getFileName().toString();
        return new KnowledgeSearchCandidate(
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                UUID.nameUUIDFromBytes((key + "-chunk").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                rank,
                UUID.nameUUIDFromBytes((key + "-version").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                document.documentId(),
                KnowledgeDocumentType.valueOf(document.type()),
                "untrusted title not used for resolution",
                document.version(),
                document.incidentFamily(),
                "payment authorization",
                "section",
                "raw content must never enter the evaluation artifact",
                fileName,
                KnowledgeSourceFormat.PDF,
                document.pdfSha256(),
                null,
                null,
                2,
                2,
                1,
                3,
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                document.approvedAt(),
                document.effectiveAt(),
                0.5f,
                rank,
                0.9f,
                rank,
                2.0d / (60 + rank));
    }

    private static KnowledgeRetrievalContext context(SynTenRetrievalEvaluationSeedMapping mapping) {
        String title;
        String description;
        String service;
        List<KnowledgeErrorCount> errors;
        if (mapping.variantId().equals("EXCLUSION")) {
            title = "Synthetic legacy knowledge exclusion probe";
            description =
                    "Synthetic exclusion probe for legacy gateway recovery, emergency routing, and AI incident automation.";
            service = null;
            errors = List.of();
        } else {
            EvaluationScenario scenario = CONTRACT.scenarios().get(mapping.variantId());
            title = scenario.title();
            description = scenario.description();
            service = scenario.evidence().serviceName();
            errors = scenario.evidence().errors().stream()
                    .map(error -> new KnowledgeErrorCount(error.errorCode(), error.count()))
                    .toList();
        }
        UUID applicable =
                List.of("AVAILABLE", "PARTIAL").contains(mapping.evidenceStatus()) ? mapping.evidenceId() : null;
        return new KnowledgeRetrievalContext(
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                mapping.investigationId(),
                mapping.incidentId(),
                SynTenRetrievalEvaluationContractRepository.INCIDENT_FAMILY,
                title,
                description,
                new KnowledgeEvidenceReference(
                        mapping.evidenceId(), mapping.evidenceStatus(), applicable, service, errors));
    }

    private static SynTenRetrievalEvaluationSeed seed() {
        AtomicLong ids = new AtomicLong(1);
        List<SynTenRetrievalEvaluationSeedMapping> mappings = new ArrayList<>();
        for (RetrievalEvaluationCase evaluationCase : CONTRACT.cases()) {
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
        SynTenRetrievalEvaluationSeed seed = new SynTenRetrievalEvaluationSeed(
                SynTenRetrievalEvaluationSeed.SCHEMA_VERSION,
                CONTRACT.evaluationVersion(),
                CONTRACT.corpusVersion(),
                "0123456789abcdef0123456789abcdef",
                EVALUATED_AT.plusSeconds(60),
                EVALUATED_AT,
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                "payment_copilot_k4_eval_test",
                mappings);
        seed.validateAgainst(CONTRACT);
        return seed;
    }

    private static String evidenceStatus(String caseId) {
        return switch (caseId) {
            case "KQ-020" -> "PARTIAL";
            case "KQ-022" -> "UNAVAILABLE";
            case "KQ-023" -> "NOT_FOUND";
            default -> "AVAILABLE";
        };
    }

    private static SynTenRetrievalEvaluationContract contract() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        return new SynTenRetrievalEvaluationContractRepository(
                        SYNTEN_ROOT.resolve("evaluation/retrieval-cases.md"),
                        Path.of(
                                        "..",
                                        "..",
                                        "syntheticIncidentGenerator",
                                        "src",
                                        "main",
                                        "resources",
                                        "scenarios",
                                        "catalog.json")
                                .toAbsolutePath()
                                .normalize(),
                        SYNTEN_ROOT.resolve("corpus"),
                        jsonMapper)
                .load(EVALUATED_AT);
    }
}
