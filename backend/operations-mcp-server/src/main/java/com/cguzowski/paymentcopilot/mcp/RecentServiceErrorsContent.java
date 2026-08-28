package com.cguzowski.paymentcopilot.mcp;

import java.time.Instant;
import java.util.List;

public record RecentServiceErrorsContent(
        String serviceName,
        Instant observedFrom,
        Instant observedTo,
        List<RecentServiceErrorObservation> errors) {

    public RecentServiceErrorsContent {
        errors = List.copyOf(errors);
    }
}
