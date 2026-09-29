package com.cguzowski.syntheticincidentgenerator.generation;

import java.time.Instant;
import java.util.UUID;

record AnswerKeyRevealEvent(
        UUID incidentId,
        UUID revealedBy,
        String terminalStatus,
        String scenarioCode,
        String oracleVersion,
        Instant revealedAt) {}
