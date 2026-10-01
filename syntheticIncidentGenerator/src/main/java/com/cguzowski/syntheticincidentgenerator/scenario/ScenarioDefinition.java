package com.cguzowski.syntheticincidentgenerator.scenario;

public record ScenarioDefinition(
        String code,
        ScenarioRarity rarity,
        String severity,
        String title,
        String description,
        ScenarioEvidence evidence,
        String incidentType) {
    public ScenarioDefinition(
            String code,
            ScenarioRarity rarity,
            String severity,
            String title,
            String description,
            ScenarioEvidence evidence) {
        this(code, rarity, severity, title, description, evidence, "AUTHORIZATION_DECLINE_RATE_SPIKE");
    }
}
