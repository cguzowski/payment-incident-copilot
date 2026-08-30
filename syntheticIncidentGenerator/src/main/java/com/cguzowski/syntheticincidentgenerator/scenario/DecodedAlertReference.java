package com.cguzowski.syntheticincidentgenerator.scenario;

import java.time.Instant;

public record DecodedAlertReference(String scenarioCode, Instant detectedAt) {}
