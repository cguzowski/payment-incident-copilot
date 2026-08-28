package com.cguzowski.paymentcopilot.mcp;

import java.util.Optional;
import java.util.UUID;

@FunctionalInterface
interface RecentServiceErrorScenarioRepository {

    Optional<RecentServiceErrorScenario> find(UUID tenantId, String scenarioReference);
}
