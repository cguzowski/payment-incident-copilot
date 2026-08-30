package com.cguzowski.paymentcopilot.report;

import java.util.List;

public record ReportDocument(
        ReportDisposition disposition,
        ReportClaim summary,
        List<ReportClaim> observations,
        List<ReportClaim> inferences,
        ReportClaim probableCause,
        ReportConfidence confidence,
        ReportClaim recommendation,
        List<ReportClaim> contradictions,
        List<ReportGap> evidenceGaps) {

    public ReportDocument {
        observations = observations == null ? List.of() : List.copyOf(observations);
        inferences = inferences == null ? List.of() : List.copyOf(inferences);
        contradictions = contradictions == null ? List.of() : List.copyOf(contradictions);
        evidenceGaps = evidenceGaps == null ? List.of() : List.copyOf(evidenceGaps);
    }
}
