package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record SynTenRetrievalEvaluationContract(
        String evaluationVersion,
        String corpusVersion,
        Instant evaluatedAt,
        List<RetrievalEvaluationCase> cases,
        Map<String, EvaluationScenario> scenarios,
        Map<String, EvaluationDocument> documents) {

    SynTenRetrievalEvaluationContract {
        cases = List.copyOf(cases);
        scenarios = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(scenarios));
        documents = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(documents));
    }

    RetrievalEvaluationCase caseById(String caseId) {
        return cases.stream()
                .filter(candidate -> candidate.caseId().equals(caseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown evaluation case: " + caseId));
    }
}
