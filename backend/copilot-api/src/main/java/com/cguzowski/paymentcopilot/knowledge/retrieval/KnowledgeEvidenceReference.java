package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;
import java.util.UUID;

record KnowledgeEvidenceReference(
        UUID latestAttemptId,
        String latestStatus,
        UUID applicableAttemptId,
        String serviceName,
        List<KnowledgeErrorCount> errorCounts) {

    KnowledgeEvidenceReference {
        errorCounts = List.copyOf(errorCounts);
    }
}
