package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

record EvaluationScenario(
        String code,
        String rarity,
        String severity,
        String title,
        String description,
        EvaluationScenarioEvidence evidence,
        EvaluationScenarioTruth truth) {}

record EvaluationScenarioEvidence(
        String availability, String statusDetail, String serviceName, List<EvaluationScenarioError> errors) {

    EvaluationScenarioEvidence {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}

record EvaluationScenarioError(String errorCode, int count, int secondsBeforeDetection) {}

record EvaluationScenarioTruth(
        String rootCause,
        String expectedDisposition,
        String expectedConfidence,
        List<String> requiredEvidence,
        String recommendation) {

    EvaluationScenarioTruth {
        requiredEvidence = requiredEvidence == null ? List.of() : List.copyOf(requiredEvidence);
    }
}
