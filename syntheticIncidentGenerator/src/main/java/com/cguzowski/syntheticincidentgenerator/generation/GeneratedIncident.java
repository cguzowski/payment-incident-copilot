package com.cguzowski.syntheticincidentgenerator.generation;

import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioRarity;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioTruth;
import java.time.Instant;
import java.util.UUID;

public record GeneratedIncident(
        UUID incidentId,
        String incidentType,
        String queueStatus,
        Instant receivedAt,
        String scenarioCode,
        ScenarioRarity rarity,
        GeneratedAlert alert,
        ScenarioTruth answerKey) {}
