package com.cguzowski.syntheticincidentgenerator.comparison;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

record FrozenOutcome(
        UUID investigationId,
        UUID reportAttemptId,
        UUID decisionId,
        String outcome,
        JsonNode report,
        JsonNode evidence,
        JsonNode decision,
        JsonNode reportAttempt) {}
