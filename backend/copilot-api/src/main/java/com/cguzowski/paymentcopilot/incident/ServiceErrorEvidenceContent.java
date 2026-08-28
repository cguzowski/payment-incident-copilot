package com.cguzowski.paymentcopilot.incident;

import java.time.Instant;
import java.util.List;

public record ServiceErrorEvidenceContent(
        String serviceName,
        Instant observedFrom,
        Instant observedTo,
        List<ServiceErrorObservation> errors) {

    public ServiceErrorEvidenceContent {
        errors = List.copyOf(errors);
    }
}
