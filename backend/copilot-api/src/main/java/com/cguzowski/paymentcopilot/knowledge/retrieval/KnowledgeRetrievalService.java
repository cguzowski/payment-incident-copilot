package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class KnowledgeRetrievalService {

    private final KnowledgeRetrievalContextAssembler contextAssembler;
    private final KnowledgeRetrievalPersistenceService persistence;
    private final KnowledgeRetrievalExecutor executor;
    private final KnowledgeRetrievalIdentifierGenerator identifiers;
    private final Clock clock;

    KnowledgeRetrievalService(
            KnowledgeRetrievalContextAssembler contextAssembler,
            KnowledgeRetrievalPersistenceService persistence,
            KnowledgeRetrievalExecutor executor,
            KnowledgeRetrievalIdentifierGenerator identifiers,
            Clock clock) {
        this.contextAssembler = contextAssembler;
        this.persistence = persistence;
        this.executor = executor;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    KnowledgeRetrievalResponse retrieve(UUID tenantId, UUID investigationId, UUID requestedBy) {
        KnowledgeRetrievalContext context = requiredContext(tenantId, investigationId);
        Instant requestedAt = Instant.now(clock);
        KnowledgeRetrievalExecutionPlan plan = executor.plan(context, requestedAt);
        KnowledgeRetrievalAttempt started = KnowledgeRetrievalAttempt.started(
                identifiers.next(),
                context,
                requestedBy,
                requestedAt,
                plan.query(),
                plan.filters(),
                plan.rankingVersion(),
                plan.rrfK(),
                plan.candidateDepth(),
                plan.minimumLexicalRank(),
                plan.minimumVectorSimilarity());
        persistence.insertStarted(started);

        KnowledgeRetrievalExecution execution = executor.execute(plan);
        KnowledgeRetrievalAttempt completed = started.complete(
                execution.status(), Instant.now(clock), execution.statusDetail(), execution.selected());
        if (!persistence.complete(completed)) {
            throw new IllegalStateException("The knowledge retrieval attempt could not be completed.");
        }
        return KnowledgeRetrievalResponse.from(completed);
    }

    List<KnowledgeRetrievalResponse> history(UUID tenantId, UUID investigationId) {
        requiredContext(tenantId, investigationId);
        return persistence.findAll(tenantId, investigationId).stream()
                .map(KnowledgeRetrievalResponse::from)
                .toList();
    }

    private KnowledgeRetrievalContext requiredContext(UUID tenantId, UUID investigationId) {
        return contextAssembler
                .find(tenantId, investigationId)
                .orElseThrow(KnowledgeInvestigationNotFoundException::new);
    }
}
