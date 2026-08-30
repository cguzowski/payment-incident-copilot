package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingMalformedException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingTimedOutException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class KnowledgeRetrievalService {

    static final String RANKING_VERSION = "postgres-hybrid-rrf/v1";
    static final int CANDIDATE_DEPTH = 20;
    static final int RRF_K = 60;
    static final float MINIMUM_LEXICAL_RANK = 0.0f;
    static final float MINIMUM_VECTOR_SIMILARITY = 0.55f;

    private final KnowledgeRetrievalContextAssembler contextAssembler;
    private final KnowledgeRetrievalPersistenceService persistence;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final KnowledgeSearchRepository searchRepository;
    private final KnowledgeContextSelector selector;
    private final KnowledgeRetrievalQueryBuilder queryBuilder;
    private final KnowledgeRetrievalIdentifierGenerator identifiers;
    private final Clock clock;

    KnowledgeRetrievalService(
            KnowledgeRetrievalContextAssembler contextAssembler,
            KnowledgeRetrievalPersistenceService persistence,
            KnowledgeEmbeddingClient embeddingClient,
            KnowledgeSearchRepository searchRepository,
            KnowledgeContextSelector selector,
            KnowledgeRetrievalQueryBuilder queryBuilder,
            KnowledgeRetrievalIdentifierGenerator identifiers,
            Clock clock) {
        this.contextAssembler = contextAssembler;
        this.persistence = persistence;
        this.embeddingClient = embeddingClient;
        this.searchRepository = searchRepository;
        this.selector = selector;
        this.queryBuilder = queryBuilder;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    KnowledgeRetrievalResponse retrieve(UUID tenantId, UUID investigationId, UUID requestedBy) {
        KnowledgeRetrievalContext context = requiredContext(tenantId, investigationId);
        DerivedKnowledgeQuery query = queryBuilder.build(context);
        Instant requestedAt = Instant.now(clock);
        KnowledgeMetadataFilters filters = new KnowledgeMetadataFilters(
                context.incidentFamily(),
                List.of(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY),
                KnowledgeApprovalStatus.APPROVED,
                requestedAt);
        KnowledgeRetrievalAttempt started = KnowledgeRetrievalAttempt.started(
                identifiers.next(),
                context,
                requestedBy,
                requestedAt,
                query,
                filters,
                RANKING_VERSION,
                RRF_K,
                CANDIDATE_DEPTH,
                MINIMUM_LEXICAL_RANK,
                MINIMUM_VECTOR_SIMILARITY);
        persistence.insertStarted(started);

        EmbeddingFailure failure = null;
        KnowledgeEmbedding queryEmbedding = null;
        try {
            queryEmbedding = embeddingClient.embed(query.text());
        } catch (KnowledgeEmbeddingUnavailableException exception) {
            failure = EmbeddingFailure.unavailable();
        } catch (KnowledgeEmbeddingTimedOutException exception) {
            failure = EmbeddingFailure.timedOut();
        } catch (KnowledgeEmbeddingMalformedException exception) {
            failure = EmbeddingFailure.malformed();
        }

        List<KnowledgeSearchCandidate> candidates = searchRepository.search(new KnowledgeSearchRequest(
                tenantId,
                context.incidentFamily(),
                requestedAt,
                query.text(),
                queryEmbedding == null ? KnowledgeEmbeddingClient.MODEL_ID : queryEmbedding.modelId(),
                queryEmbedding == null ? KnowledgeEmbeddingClient.DIMENSIONS : queryEmbedding.dimensions(),
                queryEmbedding == null ? null : queryEmbedding.vector(),
                CANDIDATE_DEPTH,
                RRF_K,
                MINIMUM_LEXICAL_RANK,
                MINIMUM_VECTOR_SIMILARITY));
        List<SelectedKnowledgeChunk> selected = selector.select(candidates);
        KnowledgeRetrievalStatus terminalStatus = terminalStatus(failure, selected);
        String statusDetail = failure == null ? null : failure.detail(!selected.isEmpty());
        KnowledgeRetrievalAttempt completed =
                started.complete(terminalStatus, Instant.now(clock), statusDetail, selected);
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

    private static KnowledgeRetrievalStatus terminalStatus(
            EmbeddingFailure failure, List<SelectedKnowledgeChunk> selected) {
        if (failure != null) {
            return selected.isEmpty() ? failure.status() : KnowledgeRetrievalStatus.PARTIAL;
        }
        return selected.isEmpty() ? KnowledgeRetrievalStatus.NO_MATCH : KnowledgeRetrievalStatus.AVAILABLE;
    }

    private record EmbeddingFailure(KnowledgeRetrievalStatus status, String partialDetail, String emptyDetail) {

        static EmbeddingFailure unavailable() {
            return new EmbeddingFailure(
                    KnowledgeRetrievalStatus.UNAVAILABLE,
                    "Query embedding was unavailable; lexical retrieval was used.",
                    "Query embedding was unavailable and lexical retrieval returned no eligible approved knowledge.");
        }

        static EmbeddingFailure timedOut() {
            return new EmbeddingFailure(
                    KnowledgeRetrievalStatus.TIMED_OUT,
                    "Query embedding timed out; lexical retrieval was used.",
                    "Query embedding timed out and lexical retrieval returned no eligible approved knowledge.");
        }

        static EmbeddingFailure malformed() {
            return new EmbeddingFailure(
                    KnowledgeRetrievalStatus.MALFORMED,
                    "Query embedding output was malformed; lexical retrieval was used.",
                    "Query embedding output was malformed and lexical retrieval returned no eligible approved knowledge.");
        }

        String detail(boolean hasResults) {
            return hasResults ? partialDetail : emptyDetail;
        }
    }
}
