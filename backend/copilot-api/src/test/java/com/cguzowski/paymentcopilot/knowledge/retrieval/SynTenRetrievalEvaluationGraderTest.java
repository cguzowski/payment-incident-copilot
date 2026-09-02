package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SynTenRetrievalEvaluationGraderTest {

    private static final Path SYNTEN_ROOT =
            Path.of("..", "..", "SynTen Inc").toAbsolutePath().normalize();
    private static final Instant EVALUATED_AT = Instant.parse("2026-09-01T12:00:00Z");
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
                    JsonMapper.builder().build())
            .load(EVALUATED_AT);

    @Test
    void passesAllFixedThresholdsForAll37IdealVariants() {
        SynTenRetrievalEvaluationGrade grade = new SynTenRetrievalEvaluationGrader().grade(CONTRACT, idealVariants());

        assertThat(grade.passed()).isTrue();
        assertThat(grade.cases()).hasSize(23);
        assertThat(grade.variants()).hasSize(37);
        assertThat(grade.aggregate().ineligibleCandidateCount()).isZero();
        assertThat(grade.aggregate().primaryRunbookCasesPassed()).isEqualTo(22);
        assertThat(grade.aggregate().supportingPolicyCasesPassed()).isEqualTo(22);
        assertThat(grade.aggregate().primaryOutranksWeakCasesPassed())
                .isEqualTo(grade.aggregate().primaryOutranksWeakApplicableCases());
        assertThat(grade.aggregate().partialSemanticsPassed()).isTrue();
        assertThat(grade.aggregate().unavailableSemanticsPassed()).isTrue();
        assertThat(grade.aggregate().supersededExclusionPassed()).isTrue();
    }

    @Test
    void appliesTheSupportingPolicyThresholdAtTheExactTwentyOfTwentyTwoBoundary() {
        List<EvaluationVariantResult> boundary =
                withoutSupportingPolicy(idealVariants(), SetOfCases.of("KQ-001", "KQ-002"));

        SynTenRetrievalEvaluationGrade passing = new SynTenRetrievalEvaluationGrader().grade(CONTRACT, boundary);
        SynTenRetrievalEvaluationGrade failing = new SynTenRetrievalEvaluationGrader()
                .grade(CONTRACT, withoutSupportingPolicy(boundary, SetOfCases.of("KQ-003")));

        assertThat(passing.aggregate().supportingPolicyCasesPassed()).isEqualTo(20);
        assertThat(passing.passed()).isTrue();
        assertThat(failing.aggregate().supportingPolicyCasesPassed()).isEqualTo(19);
        assertThat(failing.passed()).isFalse();
    }

    @Test
    void requiresEveryVariantOfAMultiScenarioCaseToSelectThePrimaryRunbook() {
        List<EvaluationVariantResult> variants = idealVariants();
        variants = replaceVariant(
                variants,
                "KQ-001",
                "S002",
                variant ->
                        withoutSelectedKey(variant, CONTRACT.caseById("KQ-001").primaryRunbookKey()));

        SynTenRetrievalEvaluationGrade grade = new SynTenRetrievalEvaluationGrader().grade(CONTRACT, variants);

        assertThat(grade.passed()).isFalse();
        assertThat(grade.aggregate().primaryRunbookCasesPassed()).isEqualTo(21);
        assertThat(grade.caseById("KQ-001").primaryRunbookSelected()).isFalse();
    }

    @Test
    void rejectsMissingRanksDuplicateCandidatesAndIneligibleSources() {
        List<EvaluationVariantResult> variants = idealVariants();
        EvaluationVariantResult original = variant(variants, "KQ-001", "S001");
        EvaluationCandidateResult first = original.candidates().getFirst();
        EvaluationCandidateResult missingRanks = first.withRanks(null, null);
        EvaluationCandidateResult duplicate = original.candidates().get(1).withChunkId(first.chunkId());
        EvaluationCandidateResult ineligible = original.candidates().get(2).withEligible(false);
        variants = replaceVariant(
                variants,
                "KQ-001",
                "S001",
                ignored -> original.withCandidates(List.of(missingRanks, duplicate, ineligible)));

        SynTenRetrievalEvaluationGrade grade = new SynTenRetrievalEvaluationGrader().grade(CONTRACT, variants);

        EvaluationVariantGrade failed = grade.variant("KQ-001", "S001");
        assertThat(failed.structureValid()).isFalse();
        assertThat(failed.noIneligibleCandidates()).isFalse();
        assertThat(failed.diagnostics())
                .anyMatch(message -> message.contains("rank"))
                .anyMatch(message -> message.contains("duplicate"))
                .anyMatch(message -> message.contains("ineligible"));
        assertThat(grade.aggregate().ineligibleCandidateCount()).isEqualTo(1);
        assertThat(grade.passed()).isFalse();
    }

    @Test
    void enforcesPartialUnavailableAndSupersededSpecialCases() {
        List<EvaluationVariantResult> variants = idealVariants();
        variants = replaceVariant(
                variants, "KQ-020", "S111", variant -> variant.withEvidence("AVAILABLE", "payment-authorization", 1));
        variants = replaceVariant(
                variants, "KQ-022", "S211", variant -> variant.withEvidence("UNAVAILABLE", "payment-authorization", 1));
        EvaluationCandidateResult superseded = candidate("RB-022", 4, null, true, new AtomicLong(10_000));
        variants = replaceVariant(
                variants,
                "KQ-023",
                "EXCLUSION",
                variant -> variant.withCandidates(append(variant.candidates(), superseded)));

        SynTenRetrievalEvaluationGrade grade = new SynTenRetrievalEvaluationGrader().grade(CONTRACT, variants);

        assertThat(grade.aggregate().partialSemanticsPassed()).isFalse();
        assertThat(grade.aggregate().unavailableSemanticsPassed()).isFalse();
        assertThat(grade.aggregate().supersededExclusionPassed()).isFalse();
        assertThat(grade.passed()).isFalse();
    }

    @Test
    void failsClosedForAMissingOrDuplicatedExpectedVariant() {
        List<EvaluationVariantResult> missing = new ArrayList<>(idealVariants());
        missing.removeLast();
        List<EvaluationVariantResult> duplicated = new ArrayList<>(idealVariants());
        duplicated.add(duplicated.getFirst());

        assertThat(new SynTenRetrievalEvaluationGrader()
                        .grade(CONTRACT, missing)
                        .passed())
                .isFalse();
        assertThat(new SynTenRetrievalEvaluationGrader()
                        .grade(CONTRACT, duplicated)
                        .passed())
                .isFalse();
    }

    private static List<EvaluationVariantResult> idealVariants() {
        AtomicLong ids = new AtomicLong(1);
        List<EvaluationVariantResult> variants = new ArrayList<>();
        for (RetrievalEvaluationCase evaluationCase : CONTRACT.cases()) {
            List<String> variantIds =
                    evaluationCase.caseId().equals("KQ-023") ? List.of("EXCLUSION") : evaluationCase.scenarioIds();
            for (String variantId : variantIds) {
                String evidenceStatus = evaluationCase.caseId().equals("KQ-020")
                        ? "PARTIAL"
                        : evaluationCase.caseId().equals("KQ-022")
                                ? "UNAVAILABLE"
                                : evaluationCase.caseId().equals("KQ-023") ? "NOT_FOUND" : "AVAILABLE";
                String service = evidenceStatus.equals("UNAVAILABLE") || evidenceStatus.equals("NOT_FOUND")
                        ? null
                        : "payment-authorization";
                int errors = service == null ? 0 : 1;
                List<EvaluationCandidateResult> candidates = new ArrayList<>();
                candidates.add(candidate(evaluationCase.primaryRunbookKey(), 1, 1, true, ids));
                candidates.add(candidate(evaluationCase.supportingPolicyKey(), 2, 2, true, ids));
                if (evaluationCase.weakApprovedMatchKey() != null) {
                    candidates.add(candidate(evaluationCase.weakApprovedMatchKey(), 3, null, true, ids));
                }
                variants.add(new EvaluationVariantResult(
                        evaluationCase.caseId(),
                        variantId,
                        evidenceStatus,
                        service,
                        errors,
                        "Observed evidence status: " + evidenceStatus,
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
        return List.copyOf(variants);
    }

    private static EvaluationCandidateResult candidate(
            String documentKey, int fusedPosition, Integer selectedPosition, boolean eligible, AtomicLong ids) {
        long id = ids.getAndIncrement();
        return new EvaluationCandidateResult(
                documentKey,
                new UUID(1L, id),
                "1.0.0",
                new UUID(2L, id),
                new UUID(3L, id),
                (int) id,
                documentKey.startsWith("PL-") ? "POLICY" : "RUNBOOK",
                eligible,
                0.5f,
                fusedPosition,
                0.9f,
                fusedPosition,
                fusedPosition,
                2.0d / (60 + fusedPosition),
                selectedPosition,
                documentKey.toLowerCase() + ".pdf",
                "PDF",
                "a".repeat(64),
                3,
                3,
                1,
                4);
    }

    private static List<EvaluationVariantResult> withoutSupportingPolicy(
            List<EvaluationVariantResult> variants, SetOfCases cases) {
        List<EvaluationVariantResult> result = variants;
        for (String caseId : cases.values()) {
            String support = CONTRACT.caseById(caseId).supportingPolicyKey();
            for (EvaluationVariantResult variant : List.copyOf(result)) {
                if (variant.caseId().equals(caseId)) {
                    result = replaceVariant(
                            result, variant.caseId(), variant.variantId(), value -> withoutSelectedKey(value, support));
                }
            }
        }
        return result;
    }

    private static EvaluationVariantResult withoutSelectedKey(EvaluationVariantResult variant, String key) {
        return variant.withCandidates(variant.candidates().stream()
                .map(candidate ->
                        candidate.documentKey().equals(key) ? candidate.withSelectedPosition(null) : candidate)
                .toList());
    }

    private static EvaluationVariantResult variant(
            List<EvaluationVariantResult> variants, String caseId, String variantId) {
        return variants.stream()
                .filter(value ->
                        value.caseId().equals(caseId) && value.variantId().equals(variantId))
                .findFirst()
                .orElseThrow();
    }

    private static List<EvaluationVariantResult> replaceVariant(
            List<EvaluationVariantResult> variants,
            String caseId,
            String variantId,
            java.util.function.UnaryOperator<EvaluationVariantResult> replacement) {
        return variants.stream()
                .map(value -> value.caseId().equals(caseId) && value.variantId().equals(variantId)
                        ? replacement.apply(value)
                        : value)
                .toList();
    }

    private static List<EvaluationCandidateResult> append(
            List<EvaluationCandidateResult> candidates, EvaluationCandidateResult added) {
        List<EvaluationCandidateResult> values = new ArrayList<>(candidates);
        values.add(added);
        return List.copyOf(values);
    }

    private record SetOfCases(List<String> values) {
        static SetOfCases of(String... values) {
            return new SetOfCases(List.of(values));
        }
    }
}
