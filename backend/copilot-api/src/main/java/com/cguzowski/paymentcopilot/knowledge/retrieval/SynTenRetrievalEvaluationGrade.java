package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

record SynTenRetrievalEvaluationGrade(
        boolean passed,
        List<EvaluationCaseGrade> cases,
        List<EvaluationVariantGrade> variants,
        EvaluationAggregateGrade aggregate,
        List<String> diagnostics) {

    SynTenRetrievalEvaluationGrade {
        cases = List.copyOf(cases);
        variants = List.copyOf(variants);
        diagnostics = List.copyOf(diagnostics);
    }

    EvaluationCaseGrade caseById(String caseId) {
        return cases.stream()
                .filter(candidate -> candidate.caseId().equals(caseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown evaluation case grade: " + caseId));
    }

    EvaluationVariantGrade variant(String caseId, String variantId) {
        return variants.stream()
                .filter(candidate -> candidate.caseId().equals(caseId)
                        && candidate.variantId().equals(variantId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown evaluation variant grade: " + caseId + "/" + variantId));
    }
}
