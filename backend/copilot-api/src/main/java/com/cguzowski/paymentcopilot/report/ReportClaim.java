package com.cguzowski.paymentcopilot.report;

import java.util.List;
import java.util.UUID;

public record ReportClaim(String statement, List<UUID> evidenceIds, List<UUID> knowledgeChunkIds) {

    public ReportClaim {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
        knowledgeChunkIds = knowledgeChunkIds == null ? List.of() : List.copyOf(knowledgeChunkIds);
    }
}
