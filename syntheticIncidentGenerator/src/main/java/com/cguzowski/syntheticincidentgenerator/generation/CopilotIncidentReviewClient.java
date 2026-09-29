package com.cguzowski.syntheticincidentgenerator.generation;

import java.util.UUID;

@FunctionalInterface
interface CopilotIncidentReviewClient {

    IncidentReviewState find(UUID incidentId);
}
