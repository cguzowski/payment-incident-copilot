package com.cguzowski.syntheticincidentgenerator.generation;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.time.Instant;
import java.util.UUID;

public record AnswerKeyRevealResponse(
        UUID incidentId,
        String terminalStatus,
        UUID revealedBy,
        Instant revealedAt,
        String oracleVersion,
        ScenarioTruth answerKey) {}
