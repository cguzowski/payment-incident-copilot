package com.cguzowski.syntheticincidentgenerator.comparison;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

record ComparisonResponse(
        UUID comparisonId,
        UUID incidentId,
        String status,
        String statusDetail,
        Instant requestedAt,
        Instant completedAt,
        UUID investigationId,
        UUID reportAttemptId,
        UUID decisionId,
        String modelId,
        String promptVersion,
        String rubricVersion,
        String oracleVersion,
        ComparisonGrade grade,
        JsonNode report) {}
