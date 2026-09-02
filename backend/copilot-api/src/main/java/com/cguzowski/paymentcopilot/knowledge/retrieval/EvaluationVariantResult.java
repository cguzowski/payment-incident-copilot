package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

record EvaluationVariantResult(
        String caseId,
        String variantId,
        String evidenceStatus,
        String evidenceServiceName,
        int evidenceErrorCount,
        String derivedQuery,
        String queryTemplateVersion,
        QueryEmbeddingStatus queryEmbeddingStatus,
        String rankingVersion,
        int rrfK,
        int candidateDepth,
        float minimumLexicalRank,
        float minimumVectorSimilarity,
        List<EvaluationCandidateResult> candidates,
        List<String> diagnostics) {

    EvaluationVariantResult {
        candidates = List.copyOf(candidates);
        diagnostics = List.copyOf(diagnostics);
    }

    EvaluationVariantResult withCandidates(List<EvaluationCandidateResult> candidates) {
        return copy(evidenceStatus, evidenceServiceName, evidenceErrorCount, candidates);
    }

    EvaluationVariantResult withEvidence(String evidenceStatus, String evidenceServiceName, int evidenceErrorCount) {
        return copy(evidenceStatus, evidenceServiceName, evidenceErrorCount, candidates);
    }

    private EvaluationVariantResult copy(
            String evidenceStatus,
            String evidenceServiceName,
            int evidenceErrorCount,
            List<EvaluationCandidateResult> candidates) {
        return new EvaluationVariantResult(
                caseId,
                variantId,
                evidenceStatus,
                evidenceServiceName,
                evidenceErrorCount,
                derivedQuery,
                queryTemplateVersion,
                queryEmbeddingStatus,
                rankingVersion,
                rrfK,
                candidateDepth,
                minimumLexicalRank,
                minimumVectorSimilarity,
                candidates,
                diagnostics);
    }
}
