package com.cguzowski.paymentcopilot.incident;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class EvidenceCollectionService {

    private final EvidenceCollectionPersistenceService persistence;
    private final ServiceErrorEvidenceGateway gateway;
    private final EvidenceIdentifierGenerator identifiers;
    private final Clock clock;

    EvidenceCollectionService(
            EvidenceCollectionPersistenceService persistence,
            ServiceErrorEvidenceGateway gateway,
            EvidenceIdentifierGenerator identifiers,
            Clock clock) {
        this.persistence = persistence;
        this.gateway = gateway;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    EvidenceCollectionResponse collect(UUID tenantId, UUID investigationId) {
        EvidenceCollectionContext context = requiredContext(tenantId, investigationId);
        UUID evidenceId = identifiers.next();
        UUID toolCallId = identifiers.next();
        EvidenceCollectionAttempt started = EvidenceCollectionAttempt.started(
                evidenceId,
                context.tenantId(),
                context.investigationId(),
                toolCallId,
                context.correlationId(),
                McpServiceErrorEvidenceGateway.SOURCE_SYSTEM,
                McpServiceErrorEvidenceGateway.SOURCE_TOOL,
                context.scenarioReference(),
                Instant.now(clock),
                McpServiceErrorEvidenceGateway.CONTENT_SCHEMA_VERSION);
        persistence.insertStarted(started);

        EvidenceSourceResult sourceResult = gateway.collect(context, toolCallId);
        EvidenceCollectionAttempt completed = started.complete(
                sourceResult.status(),
                sourceResult.retrievedAt(),
                Instant.now(clock),
                sourceResult.content(),
                sourceResult.statusDetail());
        if (!persistence.complete(completed)) {
            throw new IllegalStateException("The evidence collection attempt could not be completed.");
        }
        return EvidenceCollectionResponse.from(completed);
    }

    List<EvidenceCollectionResponse> history(UUID tenantId, UUID investigationId) {
        requiredContext(tenantId, investigationId);
        return persistence.findAll(tenantId, investigationId).stream()
                .map(EvidenceCollectionResponse::from)
                .toList();
    }

    private EvidenceCollectionContext requiredContext(UUID tenantId, UUID investigationId) {
        return persistence.findContext(tenantId, investigationId)
                .orElseThrow(InvestigationNotFoundException::new);
    }
}
