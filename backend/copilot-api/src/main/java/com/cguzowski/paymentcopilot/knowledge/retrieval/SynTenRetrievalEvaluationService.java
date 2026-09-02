package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class SynTenRetrievalEvaluationService {

    private static final String CATALOG_FINGERPRINT =
            "734461e767e08a59b83169fdf75d208d20c0366bebecd8825e2458c5f1b3d427";
    private static final String EXTRACTION_VERSION = "pdfbox-text-pages/v1";
    private static final String CHUNKING_VERSION = "pdf-page-sections/v1";
    private static final String EXCLUSION_TITLE = "Synthetic legacy knowledge exclusion probe";
    private static final String EXCLUSION_DESCRIPTION =
            "Synthetic exclusion probe for legacy gateway recovery, emergency routing, and AI incident automation.";

    private final KnowledgeRetrievalContextAssembler contextAssembler;
    private final KnowledgeRetrievalExecutor executor;
    private final SynTenRetrievalEvaluationGrader grader;
    private final Clock clock;

    @Autowired
    SynTenRetrievalEvaluationService(
            KnowledgeRetrievalContextAssembler contextAssembler, KnowledgeRetrievalExecutor executor, Clock clock) {
        this(contextAssembler, executor, new SynTenRetrievalEvaluationGrader(), clock);
    }

    SynTenRetrievalEvaluationService(
            KnowledgeRetrievalContextAssembler contextAssembler,
            KnowledgeRetrievalExecutor executor,
            SynTenRetrievalEvaluationGrader grader) {
        this(contextAssembler, executor, grader, Clock.systemUTC());
    }

    SynTenRetrievalEvaluationService(
            KnowledgeRetrievalContextAssembler contextAssembler,
            KnowledgeRetrievalExecutor executor,
            SynTenRetrievalEvaluationGrader grader,
            Clock clock) {
        this.contextAssembler = Objects.requireNonNull(contextAssembler, "contextAssembler");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.grader = Objects.requireNonNull(grader, "grader");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    SynTenRetrievalEvaluationRun evaluate(
            SynTenRetrievalEvaluationSeed seed, SynTenRetrievalEvaluationContract contract) {
        seed.validateAgainst(contract);
        Map<DocumentIdentity, EvaluationDocument> documentsById = documentsById(contract);
        List<ExecutedVariant> executed = seed.mappings().stream()
                .sorted(Comparator.comparing(SynTenRetrievalEvaluationSeedMapping::caseId)
                        .thenComparing(SynTenRetrievalEvaluationSeedMapping::variantId))
                .map(mapping -> execute(mapping, seed, contract, documentsById))
                .toList();
        List<EvaluationVariantResult> results =
                executed.stream().map(ExecutedVariant::result).toList();
        SynTenRetrievalEvaluationGrade grade = grader.grade(contract, results);
        List<SynTenRetrievalEvaluationVariantRecord> variants = executed.stream()
                .map(value -> value.record(
                        grade.variant(value.mapping().caseId(), value.mapping().variantId())))
                .toList();
        return new SynTenRetrievalEvaluationRun(
                SynTenRetrievalEvaluationRun.SCHEMA_VERSION,
                seed.runId(),
                contract.evaluationVersion(),
                contract.corpusVersion(),
                CATALOG_FINGERPRINT,
                EXTRACTION_VERSION,
                CHUNKING_VERSION,
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                contract.evaluatedAt(),
                clock.instant(),
                seed.tenantId(),
                new EvaluationThresholds(22, 20, 22, 90, 0, true, true, true),
                variants,
                grade);
    }

    private ExecutedVariant execute(
            SynTenRetrievalEvaluationSeedMapping mapping,
            SynTenRetrievalEvaluationSeed seed,
            SynTenRetrievalEvaluationContract contract,
            Map<DocumentIdentity, EvaluationDocument> documentsById) {
        KnowledgeRetrievalContext context = contextAssembler
                .find(seed.tenantId(), mapping.investigationId())
                .orElseThrow(() -> invalid("Evaluation persisted context is missing for " + key(mapping) + "."));
        validateContext(mapping, context, contract);
        KnowledgeRetrievalExecution execution = executor.execute(context, contract.evaluatedAt());
        return transform(mapping, seed, context, execution, documentsById);
    }

    private static ExecutedVariant transform(
            SynTenRetrievalEvaluationSeedMapping mapping,
            SynTenRetrievalEvaluationSeed seed,
            KnowledgeRetrievalContext context,
            KnowledgeRetrievalExecution execution,
            Map<DocumentIdentity, EvaluationDocument> documentsById) {
        Map<UUID, SelectedKnowledgeChunk> selected = selectedByChunk(execution.selected());
        List<String> diagnostics = new ArrayList<>();
        List<EvaluationCandidateResult> candidates = new ArrayList<>();
        for (int index = 0; index < execution.candidates().size(); index++) {
            KnowledgeSearchCandidate candidate = execution.candidates().get(index);
            EvaluationDocument document =
                    documentsById.get(new DocumentIdentity(candidate.documentId(), candidate.documentVersion()));
            boolean eligible = eligible(candidate, document, seed, execution.filters());
            if (!eligible) {
                diagnostics.add("Candidate " + candidate.chunkId()
                        + " could not be resolved as an eligible manifest PDF source");
            }
            SelectedKnowledgeChunk selection = selected.get(candidate.chunkId());
            candidates.add(candidateResult(candidate, document, index + 1, selection, eligible));
        }
        KnowledgeEvidenceReference evidence = context.evidence();
        int errorCount = evidence.errorCounts().stream()
                .mapToInt(KnowledgeErrorCount::count)
                .sum();
        EvaluationVariantResult result = new EvaluationVariantResult(
                mapping.caseId(),
                mapping.variantId(),
                evidence.latestStatus(),
                evidence.serviceName(),
                errorCount,
                execution.query().text(),
                execution.query().templateVersion(),
                execution.embeddingOutcome().status(),
                execution.rankingVersion(),
                execution.rrfK(),
                execution.candidateDepth(),
                execution.minimumLexicalRank(),
                execution.minimumVectorSimilarity(),
                candidates,
                diagnostics);
        EvaluationFilterResult filters = new EvaluationFilterResult(
                seed.tenantId(),
                execution.filters().incidentFamily(),
                execution.filters().documentTypes().stream().map(Enum::name).toList(),
                execution.filters().approvalStatus().name(),
                execution.filters().effectiveAt());
        List<EvaluationEvidenceError> errors = evidence.errorCounts().stream()
                .sorted(Comparator.comparing(KnowledgeErrorCount::errorCode))
                .map(error -> new EvaluationEvidenceError(error.errorCode(), error.count()))
                .toList();
        return new ExecutedVariant(
                mapping,
                errors,
                execution.query().contributingEvidenceIds(),
                filters,
                execution.embeddingOutcome(),
                result);
    }

    private static EvaluationCandidateResult candidateResult(
            KnowledgeSearchCandidate candidate,
            EvaluationDocument document,
            int fusedPosition,
            SelectedKnowledgeChunk selection,
            boolean eligible) {
        return new EvaluationCandidateResult(
                document == null ? null : document.key(),
                candidate.documentId(),
                candidate.documentVersion(),
                candidate.documentVersionId(),
                candidate.chunkId(),
                candidate.chunkOrdinal(),
                candidate.documentType().name(),
                eligible,
                candidate.lexicalRank(),
                candidate.lexicalPosition(),
                candidate.vectorSimilarity(),
                candidate.vectorPosition(),
                fusedPosition,
                candidate.fusedScore(),
                selection == null ? null : selection.selectedPosition(),
                candidate.sourceName(),
                candidate.sourceFormat().name(),
                candidate.pdfSha256(),
                candidate.sourceStartPage(),
                candidate.sourceEndPage(),
                candidate.sourceStartBlock(),
                candidate.sourceEndBlock());
    }

    private static Map<UUID, SelectedKnowledgeChunk> selectedByChunk(List<SelectedKnowledgeChunk> selected) {
        Map<UUID, SelectedKnowledgeChunk> values = new LinkedHashMap<>();
        for (SelectedKnowledgeChunk chunk : selected) {
            if (values.putIfAbsent(chunk.candidate().chunkId(), chunk) != null) {
                throw invalid("Evaluation selection contains a duplicate chunk ID.");
            }
        }
        return values;
    }

    private static Map<DocumentIdentity, EvaluationDocument> documentsById(SynTenRetrievalEvaluationContract contract) {
        Map<DocumentIdentity, EvaluationDocument> values = new HashMap<>();
        for (EvaluationDocument document : contract.documents().values()) {
            DocumentIdentity identity = new DocumentIdentity(document.documentId(), document.version());
            if (values.putIfAbsent(identity, document) != null) {
                throw invalid("Evaluation manifest contains a duplicate document ID/version.");
            }
        }
        return values;
    }

    private static boolean eligible(
            KnowledgeSearchCandidate candidate,
            EvaluationDocument document,
            SynTenRetrievalEvaluationSeed seed,
            KnowledgeMetadataFilters filters) {
        if (document == null) {
            return false;
        }
        String expectedSourceName = Path.of(document.pdf()).getFileName().toString();
        return candidate.tenantId().equals(seed.tenantId())
                && candidate.documentVersion().equals(document.version())
                && candidate.documentType().name().equals(document.type())
                && candidate.approvalStatus() == KnowledgeApprovalStatus.APPROVED
                && "APPROVED".equals(document.approvalStatus())
                && candidate.incidentFamily().equals(document.incidentFamily())
                && candidate.incidentFamily().equals(filters.incidentFamily())
                && candidate.sourceFormat() == KnowledgeSourceFormat.PDF
                && expectedSourceName.equals(candidate.sourceName())
                && document.pdfSha256().equals(candidate.pdfSha256())
                && candidate.effectiveAt().equals(document.effectiveAt())
                && !candidate.effectiveAt().isAfter(filters.effectiveAt())
                && validLocator(candidate, document);
    }

    private static boolean validLocator(KnowledgeSearchCandidate candidate, EvaluationDocument document) {
        return candidate.sourceStartLine() == null
                && candidate.sourceEndLine() == null
                && candidate.sourceStartPage() != null
                && candidate.sourceEndPage() != null
                && candidate.sourceStartPage() >= 1
                && candidate.sourceStartPage() <= candidate.sourceEndPage()
                && candidate.sourceEndPage() <= document.pageCount()
                && candidate.sourceStartBlock() != null
                && candidate.sourceEndBlock() != null
                && candidate.sourceStartBlock() >= 1
                && candidate.sourceStartBlock() <= candidate.sourceEndBlock();
    }

    private static void validateContext(
            SynTenRetrievalEvaluationSeedMapping mapping,
            KnowledgeRetrievalContext context,
            SynTenRetrievalEvaluationContract contract) {
        ExpectedContext expected = expectedContext(mapping, contract);
        KnowledgeEvidenceReference evidence = context.evidence();
        boolean matches = context.tenantId().equals(SynTenRetrievalEvaluationContractRepository.TENANT_ID)
                && context.investigationId().equals(mapping.investigationId())
                && context.incidentFamily().equals(SynTenRetrievalEvaluationContractRepository.INCIDENT_FAMILY)
                && context.incidentTitle().equals(expected.title())
                && context.incidentDescription().equals(expected.description())
                && evidence != null
                && mapping.evidenceId().equals(evidence.latestAttemptId())
                && mapping.evidenceStatus().equals(evidence.latestStatus())
                && Objects.equals(expected.serviceName(), evidence.serviceName())
                && errorCounts(evidence.errorCounts()).equals(expected.errorCounts());
        if (!matches) {
            throw invalid("Evaluation persisted context differs from the reviewed source for " + key(mapping) + ".");
        }
    }

    private static ExpectedContext expectedContext(
            SynTenRetrievalEvaluationSeedMapping mapping, SynTenRetrievalEvaluationContract contract) {
        if (mapping.variantId().equals("EXCLUSION")) {
            return new ExpectedContext(EXCLUSION_TITLE, EXCLUSION_DESCRIPTION, null, Map.of());
        }
        EvaluationScenario scenario = contract.scenarios().get(mapping.variantId());
        Map<String, Integer> errors = new LinkedHashMap<>();
        scenario.evidence().errors().stream()
                .sorted(Comparator.comparing(EvaluationScenarioError::errorCode))
                .forEach(error -> errors.put(error.errorCode(), error.count()));
        return new ExpectedContext(
                scenario.title(), scenario.description(), scenario.evidence().serviceName(), errors);
    }

    private static Map<String, Integer> errorCounts(List<KnowledgeErrorCount> errors) {
        Map<String, Integer> values = new LinkedHashMap<>();
        errors.stream()
                .sorted(Comparator.comparing(KnowledgeErrorCount::errorCode))
                .forEach(error -> {
                    if (values.putIfAbsent(error.errorCode(), error.count()) != null) {
                        throw invalid("Evaluation persisted evidence contains a duplicate error code.");
                    }
                });
        return values;
    }

    private static String key(SynTenRetrievalEvaluationSeedMapping mapping) {
        return mapping.caseId() + "/" + mapping.variantId();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private record ExpectedContext(
            String title, String description, String serviceName, Map<String, Integer> errorCounts) {}

    private record DocumentIdentity(UUID documentId, String documentVersion) {}

    private record ExecutedVariant(
            SynTenRetrievalEvaluationSeedMapping mapping,
            List<EvaluationEvidenceError> evidenceErrors,
            List<UUID> contributingEvidenceIds,
            EvaluationFilterResult filters,
            QueryEmbeddingOutcome queryEmbedding,
            EvaluationVariantResult result) {

        SynTenRetrievalEvaluationVariantRecord record(EvaluationVariantGrade assertions) {
            return new SynTenRetrievalEvaluationVariantRecord(
                    mapping.caseId(),
                    mapping.variantId(),
                    mapping.scenarioReference(),
                    mapping.incidentId(),
                    mapping.investigationId(),
                    mapping.evidenceId(),
                    evidenceErrors,
                    contributingEvidenceIds,
                    filters,
                    queryEmbedding,
                    result,
                    assertions);
        }
    }
}
