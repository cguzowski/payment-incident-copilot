package com.cguzowski.syntheticincidentgenerator.scenario;

public record ScenarioDefinition(
        String code,
        ScenarioRarity rarity,
        String severity,
        String title,
        String description,
        ScenarioEvidence evidence,
        ScenarioTruth truth) {}
