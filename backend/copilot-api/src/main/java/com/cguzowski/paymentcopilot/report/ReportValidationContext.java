package com.cguzowski.paymentcopilot.report;

import java.util.Set;
import java.util.UUID;

public record ReportValidationContext(Set<UUID> evidenceIds, Set<UUID> knowledgeChunkIds) {

    public ReportValidationContext {
        evidenceIds = evidenceIds == null ? Set.of() : Set.copyOf(evidenceIds);
        knowledgeChunkIds = knowledgeChunkIds == null ? Set.of() : Set.copyOf(knowledgeChunkIds);
    }
}
