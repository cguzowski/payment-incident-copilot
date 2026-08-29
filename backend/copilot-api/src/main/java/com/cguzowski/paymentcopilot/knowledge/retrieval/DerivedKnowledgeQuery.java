package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;
import java.util.UUID;

record DerivedKnowledgeQuery(String text, String templateVersion, List<UUID> contributingEvidenceIds) {

    DerivedKnowledgeQuery {
        contributingEvidenceIds = List.copyOf(contributingEvidenceIds);
    }
}
