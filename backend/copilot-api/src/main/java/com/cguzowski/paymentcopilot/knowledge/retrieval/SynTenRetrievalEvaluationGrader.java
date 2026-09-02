package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SynTenRetrievalEvaluationGrader {

    private static final String EXCLUSION_CASE = "KQ-023";
    private static final String PARTIAL_CASE = "KQ-020";
    private static final String UNAVAILABLE_CASE = "KQ-022";
    private static final Set<String> SUPERSEDED_KEYS = Set.of("RB-022", "PL-007", "PL-008");
    private static final int STANDARD_CASE_COUNT = 22;
    private static final int SUPPORTING_POLICY_REQUIRED = 20;

    SynTenRetrievalEvaluationGrade grade(
            SynTenRetrievalEvaluationContract contract, List<EvaluationVariantResult> results) {
        List<String> diagnostics = new ArrayList<>();
        Map<VariantKey, Integer> expected = expectedVariants(contract);
        Map<VariantKey, List<EvaluationVariantResult>> actual = new LinkedHashMap<>();
        for (EvaluationVariantResult result : results) {
            actual.computeIfAbsent(new VariantKey(result.caseId(), result.variantId()), ignored -> new ArrayList<>())
                    .add(result);
        }

        boolean expectedVariantsPassed = validateVariantCoverage(expected, actual, diagnostics);
        List<EvaluationVariantGrade> variantGrades =
                results.stream().map(result -> gradeVariant(contract, result)).toList();
        List<EvaluationCaseGrade> caseGrades = contract.cases().stream()
                .map(evaluationCase -> gradeCase(evaluationCase, variantGrades))
                .toList();

        int ineligibleCandidateCount = results.stream()
                .flatMap(result -> result.candidates().stream())
                .mapToInt(candidate -> candidate.eligible() ? 0 : 1)
                .sum();
        List<EvaluationCaseGrade> standardCases = caseGrades.stream()
                .filter(grade -> !grade.caseId().equals(EXCLUSION_CASE))
                .toList();
        int primaryPassed = count(standardCases, EvaluationCaseGrade::primaryRunbookSelected);
        int supportPassed = count(standardCases, EvaluationCaseGrade::supportingPolicySelected);
        List<EvaluationCaseGrade> weakApplicable = standardCases.stream()
                .filter(grade -> contract.caseById(grade.caseId()).weakApprovedMatchKey() != null)
                .toList();
        int weakPassed = count(weakApplicable, EvaluationCaseGrade::primaryOutranksWeakMatch);
        int weakRequired = percentageThreshold(weakApplicable.size(), 90);
        boolean partialPassed = variantsFor(PARTIAL_CASE, variantGrades).stream()
                .allMatch(EvaluationVariantGrade::partialSemanticsPreserved);
        boolean unavailablePassed = variantsFor(UNAVAILABLE_CASE, variantGrades).stream()
                .allMatch(EvaluationVariantGrade::unavailableSemanticsPreserved);
        boolean supersededPassed = variantsFor(EXCLUSION_CASE, variantGrades).stream()
                .allMatch(EvaluationVariantGrade::supersededSourcesExcluded);
        boolean structurePassed =
                variantGrades.stream().allMatch(grade -> grade.structureValid() && grade.noIneligibleCandidates());

        EvaluationAggregateGrade aggregate = new EvaluationAggregateGrade(
                contract.cases().size(),
                expected.size(),
                results.size(),
                ineligibleCandidateCount,
                primaryPassed,
                STANDARD_CASE_COUNT,
                supportPassed,
                SUPPORTING_POLICY_REQUIRED,
                weakPassed,
                weakApplicable.size(),
                weakRequired,
                partialPassed,
                unavailablePassed,
                supersededPassed,
                structurePassed,
                expectedVariantsPassed);
        boolean passed = expectedVariantsPassed
                && structurePassed
                && primaryPassed == STANDARD_CASE_COUNT
                && supportPassed >= SUPPORTING_POLICY_REQUIRED
                && weakPassed >= weakRequired
                && partialPassed
                && unavailablePassed
                && supersededPassed;
        if (!passed) {
            diagnostics.add("Evaluation did not satisfy all synten-retrieval-eval/v1 thresholds");
        }
        return new SynTenRetrievalEvaluationGrade(passed, caseGrades, variantGrades, aggregate, diagnostics);
    }

    private static Map<VariantKey, Integer> expectedVariants(SynTenRetrievalEvaluationContract contract) {
        Map<VariantKey, Integer> expected = new LinkedHashMap<>();
        for (RetrievalEvaluationCase evaluationCase : contract.cases()) {
            List<String> variantIds = evaluationCase.caseId().equals(EXCLUSION_CASE)
                    ? List.of("EXCLUSION")
                    : evaluationCase.scenarioIds();
            for (String variantId : variantIds) {
                expected.put(new VariantKey(evaluationCase.caseId(), variantId), 1);
            }
        }
        return expected;
    }

    private static boolean validateVariantCoverage(
            Map<VariantKey, Integer> expected,
            Map<VariantKey, List<EvaluationVariantResult>> actual,
            List<String> diagnostics) {
        boolean valid = true;
        for (VariantKey key : expected.keySet()) {
            int count = actual.getOrDefault(key, List.of()).size();
            if (count != 1) {
                diagnostics.add("Expected exactly one result for " + key + " but found " + count);
                valid = false;
            }
        }
        for (VariantKey key : actual.keySet()) {
            if (!expected.containsKey(key)) {
                diagnostics.add("Unexpected evaluation variant " + key);
                valid = false;
            }
        }
        return valid;
    }

    private static EvaluationVariantGrade gradeVariant(
            SynTenRetrievalEvaluationContract contract, EvaluationVariantResult result) {
        List<String> diagnostics = new ArrayList<>(result.diagnostics());
        RetrievalEvaluationCase evaluationCase;
        try {
            evaluationCase = contract.caseById(result.caseId());
        } catch (IllegalArgumentException exception) {
            diagnostics.add(exception.getMessage());
            return invalidVariant(result, diagnostics);
        }

        boolean structureValid = validateCandidateStructure(result.candidates(), diagnostics);
        boolean noIneligible = result.candidates().stream().allMatch(EvaluationCandidateResult::eligible);
        if (!noIneligible) {
            diagnostics.add("Candidate set contains an ineligible source");
        }
        boolean primarySelected = selected(result, evaluationCase.primaryRunbookKey());
        boolean supportSelected = selected(result, evaluationCase.supportingPolicyKey());
        boolean primaryOutranksWeak = primaryOutranksWeak(result, evaluationCase);
        boolean partialSemantics = !result.caseId().equals(PARTIAL_CASE) || partialSemantics(result);
        boolean unavailableSemantics = !result.caseId().equals(UNAVAILABLE_CASE) || unavailableSemantics(result);
        boolean supersededExcluded = !result.caseId().equals(EXCLUSION_CASE)
                || result.candidates().stream()
                        .noneMatch(candidate -> SUPERSEDED_KEYS.contains(candidate.documentKey()));
        if (!partialSemantics) {
            diagnostics.add("KQ-020 did not preserve PARTIAL evidence semantics");
        }
        if (!unavailableSemantics) {
            diagnostics.add("KQ-022 did not preserve UNAVAILABLE evidence semantics");
        }
        if (!supersededExcluded) {
            diagnostics.add("KQ-023 returned a superseded source");
        }
        boolean standard = !result.caseId().equals(EXCLUSION_CASE);
        boolean passed = structureValid
                && noIneligible
                && (!standard || primarySelected)
                && (!standard || supportSelected)
                && (!standard || primaryOutranksWeak)
                && partialSemantics
                && unavailableSemantics
                && supersededExcluded;
        return new EvaluationVariantGrade(
                result.caseId(),
                result.variantId(),
                structureValid,
                noIneligible,
                primarySelected,
                supportSelected,
                primaryOutranksWeak,
                partialSemantics,
                unavailableSemantics,
                supersededExcluded,
                passed,
                diagnostics);
    }

    private static EvaluationVariantGrade invalidVariant(EvaluationVariantResult result, List<String> diagnostics) {
        return new EvaluationVariantGrade(
                result.caseId(),
                result.variantId(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                diagnostics);
    }

    private static boolean validateCandidateStructure(
            List<EvaluationCandidateResult> candidates, List<String> diagnostics) {
        boolean valid = true;
        Set<java.util.UUID> chunkIds = new HashSet<>();
        Set<Integer> fusedPositions = new HashSet<>();
        Set<Integer> selectedPositions = new HashSet<>();
        for (EvaluationCandidateResult candidate : candidates) {
            if (candidate.chunkId() == null || !chunkIds.add(candidate.chunkId())) {
                diagnostics.add("Candidate set contains a duplicate or missing chunk ID");
                valid = false;
            }
            if (candidate.lexicalRank() == null && candidate.vectorSimilarity() == null) {
                diagnostics.add("Candidate has neither a lexical nor vector rank score");
                valid = false;
            }
            if (candidate.fusedPosition() <= 0 || !fusedPositions.add(candidate.fusedPosition())) {
                diagnostics.add("Candidate has a duplicate or invalid fused rank position");
                valid = false;
            }
            if (!Double.isFinite(candidate.fusedScore())) {
                diagnostics.add("Candidate has a non-finite fused score");
                valid = false;
            }
            if (candidate.selectedPosition() != null
                    && (candidate.selectedPosition() <= 0 || !selectedPositions.add(candidate.selectedPosition()))) {
                diagnostics.add("Candidate has a duplicate or invalid selection position");
                valid = false;
            }
        }
        return valid;
    }

    private static boolean selected(EvaluationVariantResult result, String documentKey) {
        return documentKey != null
                && result.candidates().stream()
                        .anyMatch(candidate -> java.util.Objects.equals(candidate.documentKey(), documentKey)
                                && candidate.selectedPosition() != null);
    }

    private static boolean primaryOutranksWeak(EvaluationVariantResult result, RetrievalEvaluationCase evaluationCase) {
        if (evaluationCase.weakApprovedMatchKey() == null) {
            return true;
        }
        Integer primaryPosition = bestFusedPosition(result, evaluationCase.primaryRunbookKey());
        Integer weakPosition = bestFusedPosition(result, evaluationCase.weakApprovedMatchKey());
        return primaryPosition != null && (weakPosition == null || primaryPosition < weakPosition);
    }

    private static Integer bestFusedPosition(EvaluationVariantResult result, String documentKey) {
        return result.candidates().stream()
                .filter(candidate -> java.util.Objects.equals(candidate.documentKey(), documentKey))
                .map(EvaluationCandidateResult::fusedPosition)
                .min(Integer::compareTo)
                .orElse(null);
    }

    private static boolean partialSemantics(EvaluationVariantResult result) {
        return "PARTIAL".equals(result.evidenceStatus())
                && result.evidenceServiceName() != null
                && result.evidenceErrorCount() > 0
                && containsIgnoreCase(result.derivedQuery(), "PARTIAL");
    }

    private static boolean unavailableSemantics(EvaluationVariantResult result) {
        return "UNAVAILABLE".equals(result.evidenceStatus())
                && result.evidenceServiceName() == null
                && result.evidenceErrorCount() == 0
                && containsIgnoreCase(result.derivedQuery(), "UNAVAILABLE");
    }

    private static boolean containsIgnoreCase(String value, String expected) {
        return value != null && value.toUpperCase(java.util.Locale.ROOT).contains(expected);
    }

    private static EvaluationCaseGrade gradeCase(
            RetrievalEvaluationCase evaluationCase, List<EvaluationVariantGrade> variants) {
        List<EvaluationVariantGrade> matching = variantsFor(evaluationCase.caseId(), variants);
        boolean primary =
                !matching.isEmpty() && matching.stream().allMatch(EvaluationVariantGrade::primaryRunbookSelected);
        boolean support =
                !matching.isEmpty() && matching.stream().allMatch(EvaluationVariantGrade::supportingPolicySelected);
        boolean weak =
                !matching.isEmpty() && matching.stream().allMatch(EvaluationVariantGrade::primaryOutranksWeakMatch);
        boolean passed = !matching.isEmpty() && matching.stream().allMatch(EvaluationVariantGrade::passed);
        return new EvaluationCaseGrade(evaluationCase.caseId(), matching.size(), primary, support, weak, passed);
    }

    private static List<EvaluationVariantGrade> variantsFor(String caseId, List<EvaluationVariantGrade> variants) {
        return variants.stream().filter(grade -> grade.caseId().equals(caseId)).toList();
    }

    private static int count(
            List<EvaluationCaseGrade> cases, java.util.function.Predicate<EvaluationCaseGrade> predicate) {
        return (int) cases.stream().filter(predicate).count();
    }

    private static int percentageThreshold(int count, int percentage) {
        return (count * percentage + 99) / 100;
    }

    private record VariantKey(String caseId, String variantId) {
        @Override
        public String toString() {
            return caseId + "/" + variantId;
        }
    }
}
