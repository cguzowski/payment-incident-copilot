package com.cguzowski.paymentcopilot.decision;

import java.util.Optional;
import java.util.UUID;

interface HumanDecisionRepository {

    boolean insertIfAbsent(HumanDecision decision);

    Optional<HumanDecision> find(UUID tenantId, UUID investigationId);
}
