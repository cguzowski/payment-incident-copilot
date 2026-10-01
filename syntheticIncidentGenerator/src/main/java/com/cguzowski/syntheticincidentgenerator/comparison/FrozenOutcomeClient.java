package com.cguzowski.syntheticincidentgenerator.comparison;

import java.util.UUID;

interface FrozenOutcomeClient {
    FrozenOutcome find(UUID incidentId, String terminalStatus);
}
