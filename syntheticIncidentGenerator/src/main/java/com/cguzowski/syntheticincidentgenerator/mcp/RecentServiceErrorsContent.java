package com.cguzowski.syntheticincidentgenerator.mcp;

import java.time.Instant;
import java.util.List;

public record RecentServiceErrorsContent(
        String serviceName, Instant observedFrom, Instant observedTo, List<RecentServiceErrorObservation> errors) {}
