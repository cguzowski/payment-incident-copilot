package com.cguzowski.paymentcopilot.evidence;

import java.util.List;
import java.util.UUID;

public record EvidenceSnapshot(
        UUID latestAttemptId,
        String latestStatus,
        UUID applicableAttemptId,
        String serviceName,
        List<EvidenceErrorCount> errorCounts) {

    public EvidenceSnapshot {
        errorCounts = List.copyOf(errorCounts);
    }
}
