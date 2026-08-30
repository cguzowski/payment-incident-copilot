package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;
import java.util.UUID;

public record ReportKnowledgeSnapshot(UUID retrievalId, String status, List<ReportKnowledgeChunk> chunks) {

    public ReportKnowledgeSnapshot {
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
    }
}
