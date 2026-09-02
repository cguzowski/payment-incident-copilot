package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

record EvaluationVariantGrade(
        String caseId,
        String variantId,
        boolean structureValid,
        boolean noIneligibleCandidates,
        boolean primaryRunbookSelected,
        boolean supportingPolicySelected,
        boolean primaryOutranksWeakMatch,
        boolean partialSemanticsPreserved,
        boolean unavailableSemanticsPreserved,
        boolean supersededSourcesExcluded,
        boolean passed,
        List<String> diagnostics) {

    EvaluationVariantGrade {
        diagnostics = List.copyOf(diagnostics);
    }
}
