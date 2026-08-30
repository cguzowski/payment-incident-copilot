package com.cguzowski.syntheticincidentgenerator.scenario;

import java.util.List;

public record ScenarioEvidence(
        EvidenceAvailability availability, String statusDetail, String serviceName, List<ScenarioError> errors) {}
