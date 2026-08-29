package com.cguzowski.paymentcopilot.report;

import java.util.List;
import java.util.UUID;

public record ReportConfidence(ReportConfidenceLevel level, String rationale, List<UUID> evidenceIds) {

    public ReportConfidence {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
