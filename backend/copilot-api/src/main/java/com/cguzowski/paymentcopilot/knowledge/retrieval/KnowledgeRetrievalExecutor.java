package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbedding;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingClient;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingMalformedException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingTimedOutException;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeEmbeddingUnavailableException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class KnowledgeRetrievalExecutor {

    private static final String RANKING_VERSION = "postgres-hybrid-rrf/v2";
    private static final int CANDIDATE_DEPTH = 20;
    private static final int RRF_K = 60;
    private static final float MINIMUM_LEXICAL_RANK = 0.0f;
    private static final float MINIMUM_VECTOR_SIMILARITY = 0.55f;

    private final KnowledgeEmbeddingClient embeddingClient;
    private final KnowledgeSearchRepository searchRepository;
    private final KnowledgeContextSelector selector;
    private final KnowledgeRetrievalQueryBuilder queryBuilder;

    KnowledgeRetrievalExecutor(
            KnowledgeEmbeddingClient embeddingClient,
            KnowledgeSearchRepository searchRepository,
            KnowledgeContextSelector selector,
            KnowledgeRetrievalQueryBuilder queryBuilder) {
        this.embeddingClient = embeddingClient;
        this.searchRepository = searchRepository;
        this.selector = selector;
        this.queryBuilder = queryBuilder;
    }

    KnowledgeRetrievalExecution execute(KnowledgeRetrievalContext context, Instant effectiveAt) {
        return execute(plan(context, effectiveAt));
    }

    KnowledgeRetrievalExecutionPlan plan(KnowledgeRetrievalContext context, Instant effectiveAt) {
        DerivedKnowledgeQuery query = queryBuilder.build(context);
        KnowledgeMetadataFilters filters = new KnowledgeMetadataFilters(
                context.incidentFamily(),
                List.of(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY),
                KnowledgeApprovalStatus.APPROVED,
                effectiveAt);
        return new KnowledgeRetrievalExecutionPlan(
                context.tenantId(),
                query,
                filters,
                RANKING_VERSION,
                RRF_K,
                CANDIDATE_DEPTH,
                MINIMUM_LEXICAL_RANK,
                MINIMUM_VECTOR_SIMILARITY);
    }

    KnowledgeRetrievalExecution execute(KnowledgeRetrievalExecutionPlan plan) {
        EmbeddingResult embedding = embed(plan.query().text());
        List<KnowledgeSearchCandidate> candidates = searchRepository.search(new KnowledgeSearchRequest(
                plan.tenantId(),
                plan.filters().incidentFamily(),
                plan.filters().effectiveAt(),
                plan.query().text(),
                embedding.embedding() == null
                        ? KnowledgeEmbeddingClient.MODEL_ID
                        : embedding.embedding().modelId(),
                embedding.embedding() == null
                        ? KnowledgeEmbeddingClient.DIMENSIONS
                        : embedding.embedding().dimensions(),
                embedding.embedding() == null ? null : embedding.embedding().vector(),
                plan.candidateDepth(),
                plan.rrfK(),
                plan.minimumLexicalRank(),
                plan.minimumVectorSimilarity()));
        List<SelectedKnowledgeChunk> selected = selector.select(candidates);
        KnowledgeRetrievalStatus terminalStatus =
                terminalStatus(embedding.outcome().status(), selected);
        String statusDetail = statusDetail(embedding.outcome().status(), !selected.isEmpty());
        return new KnowledgeRetrievalExecution(
                plan.query(),
                plan.filters(),
                plan.rankingVersion(),
                plan.rrfK(),
                plan.candidateDepth(),
                plan.minimumLexicalRank(),
                plan.minimumVectorSimilarity(),
                embedding.outcome(),
                candidates,
                selected,
                terminalStatus,
                statusDetail);
    }

    private EmbeddingResult embed(String queryText) {
        try {
            KnowledgeEmbedding embedding = embeddingClient.embed(queryText);
            return new EmbeddingResult(
                    embedding,
                    new QueryEmbeddingOutcome(
                            QueryEmbeddingStatus.AVAILABLE,
                            embedding.modelId(),
                            embedding.dimensions(),
                            embedding.normalized()));
        } catch (KnowledgeEmbeddingUnavailableException exception) {
            return failedEmbedding(QueryEmbeddingStatus.UNAVAILABLE);
        } catch (KnowledgeEmbeddingTimedOutException exception) {
            return failedEmbedding(QueryEmbeddingStatus.TIMED_OUT);
        } catch (KnowledgeEmbeddingMalformedException exception) {
            return failedEmbedding(QueryEmbeddingStatus.MALFORMED);
        }
    }

    private static EmbeddingResult failedEmbedding(QueryEmbeddingStatus status) {
        return new EmbeddingResult(
                null,
                new QueryEmbeddingOutcome(
                        status, KnowledgeEmbeddingClient.MODEL_ID, KnowledgeEmbeddingClient.DIMENSIONS, false));
    }

    private static KnowledgeRetrievalStatus terminalStatus(
            QueryEmbeddingStatus embeddingStatus, List<SelectedKnowledgeChunk> selected) {
        if (embeddingStatus != QueryEmbeddingStatus.AVAILABLE) {
            if (!selected.isEmpty()) {
                return KnowledgeRetrievalStatus.PARTIAL;
            }
            return switch (embeddingStatus) {
                case UNAVAILABLE -> KnowledgeRetrievalStatus.UNAVAILABLE;
                case TIMED_OUT -> KnowledgeRetrievalStatus.TIMED_OUT;
                case MALFORMED -> KnowledgeRetrievalStatus.MALFORMED;
                case AVAILABLE -> throw new IllegalStateException("Available embedding cannot be a failure.");
            };
        }
        return selected.isEmpty() ? KnowledgeRetrievalStatus.NO_MATCH : KnowledgeRetrievalStatus.AVAILABLE;
    }

    private static String statusDetail(QueryEmbeddingStatus status, boolean hasResults) {
        return switch (status) {
            case AVAILABLE -> null;
            case UNAVAILABLE ->
                hasResults
                        ? "Query embedding was unavailable; lexical retrieval was used."
                        : "Query embedding was unavailable and lexical retrieval returned no eligible approved knowledge.";
            case TIMED_OUT ->
                hasResults
                        ? "Query embedding timed out; lexical retrieval was used."
                        : "Query embedding timed out and lexical retrieval returned no eligible approved knowledge.";
            case MALFORMED ->
                hasResults
                        ? "Query embedding output was malformed; lexical retrieval was used."
                        : "Query embedding output was malformed and lexical retrieval returned no eligible approved knowledge.";
        };
    }

    private record EmbeddingResult(KnowledgeEmbedding embedding, QueryEmbeddingOutcome outcome) {}
}
