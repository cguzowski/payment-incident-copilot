package com.cguzowski.syntheticincidentgenerator.scenario;

import java.util.List;

public record ScenarioTruth(
        String rootCause,
        String expectedDisposition,
        String expectedConfidence,
        List<String> requiredEvidence,
        String recommendation,
        String decisionRule) {}
