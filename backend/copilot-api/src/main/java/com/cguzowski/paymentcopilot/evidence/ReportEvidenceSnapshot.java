package com.cguzowski.paymentcopilot.evidence;

import java.util.List;
import java.util.UUID;

public record ReportEvidenceSnapshot(
        UUID latestAttemptId,
        String latestStatus,
        UUID applicableAttemptId,
        String serviceName,
        List<ReportEvidenceObservation> observations) {

    public ReportEvidenceSnapshot {
        observations = observations == null ? List.of() : List.copyOf(observations);
    }
}
