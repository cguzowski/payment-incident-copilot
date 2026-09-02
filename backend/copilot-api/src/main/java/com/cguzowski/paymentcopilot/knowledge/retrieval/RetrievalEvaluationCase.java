package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

record RetrievalEvaluationCase(
        String caseId,
        List<String> scenarioIds,
        String querySignals,
        String primaryRunbookKey,
        String supportingPolicyKey,
        String weakApprovedMatchKey,
        String expectedReportPosture) {

    RetrievalEvaluationCase {
        scenarioIds = List.copyOf(scenarioIds);
    }
}
