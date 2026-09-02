package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class SynTenRetrievalEvaluationArtifactWriterTest {

    private static final Path SYNTEN_ROOT =
            Path.of("..", "..", "SynTen Inc").toAbsolutePath().normalize();
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");
    private static final JsonMapper JSON =
            JsonMapper.builder().findAndAddModules().build();
    private static final SynTenRetrievalEvaluationContract CONTRACT = new SynTenRetrievalEvaluationContractRepository(
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
                    JSON)
            .load(EVALUATED_AT);

    @TempDir
    Path temporaryDirectory;

    @Test
    void atomicallyWritesCompletePassingAndFailingArtifactsWithoutSensitivePayloads() throws Exception {
        SynTenRetrievalEvaluationArtifactWriter passingWriter =
                new SynTenRetrievalEvaluationArtifactWriter(temporaryDirectory.resolve("passing"), JSON);
        SynTenRetrievalEvaluationArtifactWriter failingWriter =
                new SynTenRetrievalEvaluationArtifactWriter(temporaryDirectory.resolve("failing"), JSON);

        Path passing = passingWriter.write(run(true));
        Path failing = failingWriter.write(run(false));

        String passingJson = Files.readString(passing);
        String failingJson = Files.readString(failing);
        assertThat(passing.getFileName().toString()).endsWith("-PASS.json");
        assertThat(failing.getFileName().toString()).endsWith("-FAIL.json");
        assertThat(passingJson)
                .contains("synten-retrieval-eval-result/v1")
                .contains("\"actualVariantCount\" : 37")
                .contains("\"sourceStartPage\" : 3")
                .doesNotContain("raw PDF text")
                .doesNotContain("embeddingInput")
                .doesNotContain("rawContent")
                .doesNotContain("databaseUrl")
                .doesNotContain("stackTrace");
        assertThat(failingJson).contains("\"passed\" : false");
        assertThat(Files.list(passing.getParent())
                        .map(path -> path.getFileName().toString()))
                .noneMatch(name -> name.contains(".tmp-"));
    }

    @Test
    void producesDeterministicJsonAndNeverOverwritesAnExistingRun() throws Exception {
        SynTenRetrievalEvaluationRun run = run(true);
        SynTenRetrievalEvaluationArtifactWriter first =
                new SynTenRetrievalEvaluationArtifactWriter(temporaryDirectory.resolve("first"), JSON);
        SynTenRetrievalEvaluationArtifactWriter second =
                new SynTenRetrievalEvaluationArtifactWriter(temporaryDirectory.resolve("second"), JSON);

        Path firstPath = first.write(run);
        Path secondPath = second.write(run);
        byte[] original = Files.readAllBytes(firstPath);

        assertThat(Files.readAllBytes(secondPath)).isEqualTo(original);
        assertThatThrownBy(() -> first.write(run)).hasMessageContaining("already exists");
        assertThat(Files.readAllBytes(firstPath)).isEqualTo(original);
    }

    @Test
    void validatesTheCompleteResultBeforeCreatingAnyOutput() {
        SynTenRetrievalEvaluationRun incomplete = new SynTenRetrievalEvaluationRun(
                SynTenRetrievalEvaluationRun.SCHEMA_VERSION,
                "0123456789abcdef0123456789abcdef",
                CONTRACT.evaluationVersion(),
                CONTRACT.corpusVersion(),
                "a".repeat(64),
                "pdfbox-text-pages/v1",
                "pdf-page-sections/v1",
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                EVALUATED_AT,
                EVALUATED_AT.plusSeconds(1),
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                new EvaluationThresholds(22, 20, 22, 90, 0, true, true, true),
                List.of(),
                new SynTenRetrievalEvaluationGrader().grade(CONTRACT, List.of()));
        Path output = temporaryDirectory.resolve("invalid");

        assertThatThrownBy(() -> new SynTenRetrievalEvaluationArtifactWriter(output, JSON).write(incomplete))
                .hasMessageContaining("complete");
        assertThat(output).doesNotExist();
    }

    @Test
    void acceptsTheFullDisjointPerTypeLexicalAndVectorCandidateUnion() {
        SynTenRetrievalEvaluationRun run = runWithFirstVariantCandidateCount(80);

        Path written =
                new SynTenRetrievalEvaluationArtifactWriter(temporaryDirectory.resolve("full-union"), JSON).write(run);

        assertThat(written).exists();
    }

    @Test
    void acceptsACompleteEvaluationWhenEveryVariantUsesTheFullCandidateUnion() throws Exception {
        SynTenRetrievalEvaluationRun run = runWithEveryVariantCandidateCount(80);

        Path written = new SynTenRetrievalEvaluationArtifactWriter(
                        temporaryDirectory.resolve("full-evaluation-union"), JSON)
                .write(run);

        assertThat(written).exists();
        assertThat(Files.size(written)).isGreaterThan(2_000_000L).isLessThanOrEqualTo(4_000_000L);
    }

    @Test
    void rejectsACandidateUnionLargerThanAllPerTypeRankedLists() {
        SynTenRetrievalEvaluationRun run = runWithFirstVariantCandidateCount(81);
        Path output = temporaryDirectory.resolve("oversized-union");

        assertThatThrownBy(() -> new SynTenRetrievalEvaluationArtifactWriter(output, JSON).write(run))
                .hasMessageContaining("complete or deterministically ordered");
        assertThat(output).doesNotExist();
    }

    private static SynTenRetrievalEvaluationRun runWithFirstVariantCandidateCount(int candidateCount) {
        List<EvaluationVariantResult> results = new ArrayList<>(idealResults());
        EvaluationVariantResult first = results.getFirst();
        List<EvaluationCandidateResult> candidates = new ArrayList<>(first.candidates());
        AtomicLong ids = new AtomicLong(10_000);
        while (candidates.size() < candidateCount) {
            candidates.add(
                    candidate(CONTRACT.caseById(first.caseId()).primaryRunbookKey(), candidates.size() + 1, null, ids));
        }
        results.set(0, first.withCandidates(candidates));
        return run(results);
    }

    private static SynTenRetrievalEvaluationRun runWithEveryVariantCandidateCount(int candidateCount) {
        AtomicLong ids = new AtomicLong(20_000);
        List<EvaluationVariantResult> results = idealResults().stream()
                .map(result -> {
                    List<EvaluationCandidateResult> candidates = new ArrayList<>(result.candidates());
                    while (candidates.size() < candidateCount) {
                        candidates.add(candidate(
                                CONTRACT.caseById(result.caseId()).primaryRunbookKey(),
                                candidates.size() + 1,
                                null,
                                ids));
                    }
                    return result.withCandidates(candidates);
                })
                .toList();
        return run(results);
    }

    private static SynTenRetrievalEvaluationRun run(boolean passing) {
        List<EvaluationVariantResult> results = idealResults();
        if (!passing) {
            EvaluationVariantResult first = results.getFirst();
            String primary = CONTRACT.caseById(first.caseId()).primaryRunbookKey();
            List<EvaluationCandidateResult> candidates = first.candidates().stream()
                    .map(candidate ->
                            candidate.documentKey().equals(primary) ? candidate.withSelectedPosition(null) : candidate)
                    .toList();
            List<EvaluationVariantResult> changed = new ArrayList<>(results);
            changed.set(0, first.withCandidates(candidates));
            results = List.copyOf(changed);
        }
        return run(results);
    }

    private static SynTenRetrievalEvaluationRun run(List<EvaluationVariantResult> results) {
        SynTenRetrievalEvaluationGrade grade = new SynTenRetrievalEvaluationGrader().grade(CONTRACT, results);
        AtomicLong ids = new AtomicLong(1);
        List<SynTenRetrievalEvaluationVariantRecord> variants = results.stream()
                .map(result -> {
                    long id = ids.getAndIncrement();
                    String scenario = result.variantId().equals("EXCLUSION") ? "S999" : result.variantId();
                    return new SynTenRetrievalEvaluationVariantRecord(
                            result.caseId(),
                            result.variantId(),
                            "sig-v1-" + scenario + "-1788264000-0123456789ab",
                            new UUID(1, id),
                            new UUID(2, id),
                            new UUID(3, id),
                            List.of(new EvaluationEvidenceError("E-TEST", 1)),
                            List.of(new UUID(3, id)),
                            new EvaluationFilterResult(
                                    SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                                    SynTenRetrievalEvaluationContractRepository.INCIDENT_FAMILY,
                                    List.of("RUNBOOK", "POLICY"),
                                    "APPROVED",
                                    EVALUATED_AT),
                            new QueryEmbeddingOutcome(
                                    QueryEmbeddingStatus.AVAILABLE,
                                    KnowledgeEmbeddingClient.MODEL_ID,
                                    KnowledgeEmbeddingClient.DIMENSIONS,
                                    true),
                            result,
                            grade.variant(result.caseId(), result.variantId()));
                })
                .toList();
        return new SynTenRetrievalEvaluationRun(
                SynTenRetrievalEvaluationRun.SCHEMA_VERSION,
                "0123456789abcdef0123456789abcdef",
                CONTRACT.evaluationVersion(),
                CONTRACT.corpusVersion(),
                "734461e767e08a59b83169fdf75d208d20c0366bebecd8825e2458c5f1b3d427",
                "pdfbox-text-pages/v1",
                "pdf-page-sections/v1",
                KnowledgeEmbeddingClient.MODEL_ID,
                KnowledgeEmbeddingClient.DIMENSIONS,
                EVALUATED_AT,
                EVALUATED_AT.plusSeconds(60),
                SynTenRetrievalEvaluationContractRepository.TENANT_ID,
                new EvaluationThresholds(22, 20, 22, 90, 0, true, true, true),
                variants,
                grade);
    }

    private static List<EvaluationVariantResult> idealResults() {
        AtomicLong ids = new AtomicLong(1);
        List<EvaluationVariantResult> results = new ArrayList<>();
        for (RetrievalEvaluationCase evaluationCase : CONTRACT.cases()) {
            List<String> variants =
                    evaluationCase.caseId().equals("KQ-023") ? List.of("EXCLUSION") : evaluationCase.scenarioIds();
            for (String variant : variants) {
                String status = evidenceStatus(evaluationCase.caseId());
                List<EvaluationCandidateResult> candidates = new ArrayList<>();
                candidates.add(candidate(evaluationCase.primaryRunbookKey(), 1, 1, ids));
                candidates.add(candidate(evaluationCase.supportingPolicyKey(), 2, 2, ids));
                if (evaluationCase.weakApprovedMatchKey() != null) {
                    candidates.add(candidate(evaluationCase.weakApprovedMatchKey(), 3, null, ids));
                }
                results.add(new EvaluationVariantResult(
                        evaluationCase.caseId(),
                        variant,
                        status,
                        List.of("UNAVAILABLE", "NOT_FOUND").contains(status) ? null : "payment-authorization",
                        List.of("UNAVAILABLE", "NOT_FOUND").contains(status) ? 0 : 1,
                        "Observed evidence status: " + status,
                        "knowledge-query/v1",
                        QueryEmbeddingStatus.AVAILABLE,
                        "postgres-hybrid-rrf/v1",
                        60,
                        20,
                        0.0f,
                        0.55f,
                        candidates,
                        List.of()));
            }
        }
        return List.copyOf(results);
    }

    private static EvaluationCandidateResult candidate(
            String key, int fusedPosition, Integer selectedPosition, AtomicLong ids) {
        long id = ids.getAndIncrement();
        EvaluationDocument document = CONTRACT.documents().get(key);
        return new EvaluationCandidateResult(
                key,
                document.documentId(),
                document.version(),
                new UUID(2, id),
                new UUID(3, id),
                (int) id,
                document.type(),
                true,
                0.5f,
                fusedPosition,
                0.9f,
                fusedPosition,
                fusedPosition,
                2.0d / (60 + fusedPosition),
                selectedPosition,
                Path.of(document.pdf()).getFileName().toString(),
                "PDF",
                document.pdfSha256(),
                3,
                3,
                1,
                4);
    }

    private static String evidenceStatus(String caseId) {
        return switch (caseId) {
            case "KQ-020" -> "PARTIAL";
            case "KQ-022" -> "UNAVAILABLE";
            case "KQ-023" -> "NOT_FOUND";
            default -> "AVAILABLE";
        };
    }
}
