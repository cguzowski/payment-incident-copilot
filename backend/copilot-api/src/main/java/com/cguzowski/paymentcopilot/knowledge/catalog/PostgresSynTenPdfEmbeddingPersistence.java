package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class PostgresSynTenPdfEmbeddingPersistence implements SynTenPdfEmbeddingPersistence {

    private final JdbcClient jdbcClient;

    PostgresSynTenPdfEmbeddingPersistence(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SynTenPdfEmbeddingOperationSummary persist(
            List<PreparedSynTenPdfEmbedding> preparedEmbeddings, Instant embeddedAt) throws IllegalStateException {
        Map<UUID, PreparedSynTenPdfEmbedding> preparedByChunkId = validate(preparedEmbeddings, embeddedAt);
        PreparedSynTenPdfEmbedding first = preparedEmbeddings.getFirst();
        List<LockedChunk> lockedChunks = lockChunks(first.target().tenantId(), preparedByChunkId.keySet());
        compareWithPrepared(preparedByChunkId, lockedChunks);

        SynTenPdfEmbeddingState initialState = SynTenPdfEmbeddingStateMachine.classify(
                lockedChunks.stream().map(LockedChunk::embeddingMetadata).toList());
        if (initialState == SynTenPdfEmbeddingState.COMPLETE_SAME_MODEL) {
            return summary(initialState, SynTenPdfCatalogPlanner.EXPECTED_CHUNK_COUNT, true);
        }
        if (initialState != SynTenPdfEmbeddingState.ABSENT) {
            throw new IllegalStateException(
                    "SynTen PDF embeddings are not safely backfillable from state " + initialState + ".");
        }

        for (PreparedSynTenPdfEmbedding prepared : preparedEmbeddings) {
            updateChunk(prepared, embeddedAt);
        }
        return summary(initialState, 0, false);
    }

    private List<LockedChunk> lockChunks(UUID tenantId, Set<UUID> chunkIds) {
        return jdbcClient
                .sql("""
                        SELECT chunk.id AS chunk_id,
                               chunk.tenant_id,
                               chunk.document_version_id,
                               document.source_format,
                               chunk.embedding_input_hash,
                               chunk.embedding_model_id,
                               chunk.embedding_dimensions,
                               chunk.embedding_normalized,
                               chunk.embedded_at,
                               vector_dims(chunk.embedding) AS vector_dimensions,
                               vector_norm(chunk.embedding) AS vector_norm
                        FROM knowledge_chunk chunk
                        JOIN knowledge_document_version document
                          ON document.tenant_id = chunk.tenant_id
                         AND document.id = chunk.document_version_id
                        WHERE chunk.tenant_id = :tenantId
                          AND chunk.id IN (:chunkIds)
                        ORDER BY chunk.id
                        FOR UPDATE OF chunk
                        """)
                .param("tenantId", tenantId)
                .param("chunkIds", chunkIds)
                .query((resultSet, rowNumber) -> new LockedChunk(
                        resultSet.getObject("chunk_id", UUID.class),
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getObject("document_version_id", UUID.class),
                        resultSet.getString("source_format"),
                        resultSet.getString("embedding_input_hash"),
                        resultSet.getString("embedding_model_id"),
                        resultSet.getObject("embedding_dimensions", Integer.class),
                        resultSet.getObject("embedding_normalized", Boolean.class),
                        instant(resultSet.getObject("embedded_at", OffsetDateTime.class)),
                        resultSet.getObject("vector_dimensions", Integer.class),
                        resultSet.getObject("vector_norm", Double.class)))
                .list();
    }

    private static Map<UUID, PreparedSynTenPdfEmbedding> validate(
            List<PreparedSynTenPdfEmbedding> preparedEmbeddings, Instant embeddedAt) {
        Objects.requireNonNull(preparedEmbeddings, "preparedEmbeddings");
        Objects.requireNonNull(embeddedAt, "embeddedAt");
        if (preparedEmbeddings.size() != SynTenPdfCatalogPlanner.EXPECTED_CHUNK_COUNT) {
            throw new IllegalArgumentException("The SynTen PDF backfill must prepare exactly 705 embeddings.");
        }

        Map<UUID, PreparedSynTenPdfEmbedding> preparedByChunkId = new HashMap<>();
        UUID tenantId = null;
        for (PreparedSynTenPdfEmbedding prepared : preparedEmbeddings) {
            if (prepared == null || prepared.target() == null) {
                throw new IllegalArgumentException("A prepared SynTen PDF embedding is null.");
            }
            SynTenPdfEmbeddingTarget target = prepared.target();
            if (!SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT.equals(target.catalogFingerprint())) {
                throw new IllegalArgumentException("The prepared embeddings do not match the accepted K3 catalog.");
            }
            if (tenantId == null) {
                tenantId = target.tenantId();
            } else if (!tenantId.equals(target.tenantId())) {
                throw new IllegalArgumentException("The prepared embeddings contain more than one tenant.");
            }
            if (preparedByChunkId.put(target.chunkId(), prepared) != null) {
                throw new IllegalArgumentException("The prepared embeddings contain a duplicate chunk ID.");
            }
            validateEmbedding(prepared);
        }
        return preparedByChunkId;
    }

    private static void validateEmbedding(PreparedSynTenPdfEmbedding prepared) {
        if (!KnowledgeEmbeddingClient.MODEL_ID.equals(prepared.modelId())
                || prepared.dimensions() != KnowledgeEmbeddingClient.DIMENSIONS
                || !prepared.normalized()) {
            throw new IllegalArgumentException("A prepared SynTen PDF embedding has incompatible metadata.");
        }
        float[] vector = prepared.vector();
        if (vector.length != KnowledgeEmbeddingClient.DIMENSIONS) {
            throw new IllegalArgumentException("A prepared SynTen PDF embedding has incompatible dimensions.");
        }
        double squaredNorm = 0.0d;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("A prepared SynTen PDF embedding contains a non-finite value.");
            }
            squaredNorm += (double) value * value;
        }
        if (Math.abs(Math.sqrt(squaredNorm) - 1.0d) > 0.01d) {
            throw new IllegalArgumentException("A prepared SynTen PDF embedding is not normalized.");
        }
    }

    private static void compareWithPrepared(
            Map<UUID, PreparedSynTenPdfEmbedding> preparedByChunkId, List<LockedChunk> lockedChunks) {
        if (lockedChunks.size() != SynTenPdfCatalogPlanner.EXPECTED_CHUNK_COUNT) {
            throw catalogChanged();
        }
        Set<UUID> seen = new HashSet<>();
        for (LockedChunk locked : lockedChunks) {
            PreparedSynTenPdfEmbedding prepared = preparedByChunkId.get(locked.chunkId());
            if (prepared == null || !seen.add(locked.chunkId())) {
                throw catalogChanged();
            }
            SynTenPdfEmbeddingTarget target = prepared.target();
            if (!Objects.equals(target.tenantId(), locked.tenantId())
                    || !Objects.equals(target.documentVersionId(), locked.documentVersionId())
                    || !KnowledgeSourceFormat.PDF.name().equals(locked.sourceFormat())
                    || !Objects.equals(target.embeddingInputHash(), locked.embeddingInputHash())) {
                throw catalogChanged();
            }
        }
        if (seen.size() != preparedByChunkId.size()) {
            throw catalogChanged();
        }
    }

    private void updateChunk(PreparedSynTenPdfEmbedding prepared, Instant embeddedAt) {
        SynTenPdfEmbeddingTarget target = prepared.target();
        int updated = jdbcClient
                .sql("""
                        UPDATE knowledge_chunk
                        SET embedding_model_id = :modelId,
                            embedding_dimensions = :dimensions,
                            embedding_normalized = TRUE,
                            embedded_at = :embeddedAt,
                            embedding = CAST(:embedding AS vector)
                        WHERE tenant_id = :tenantId
                          AND id = :chunkId
                          AND document_version_id = :documentVersionId
                          AND embedding_input_hash = :embeddingInputHash
                          AND embedding_model_id IS NULL
                          AND embedding_dimensions IS NULL
                          AND embedding_normalized IS NULL
                          AND embedded_at IS NULL
                          AND embedding IS NULL
                        """)
                .param("modelId", prepared.modelId())
                .param("dimensions", prepared.dimensions())
                .param("embeddedAt", utc(embeddedAt))
                .param("embedding", PostgresKnowledgeIndexRepository.vectorLiteral(prepared.vector()))
                .param("tenantId", target.tenantId())
                .param("chunkId", target.chunkId())
                .param("documentVersionId", target.documentVersionId())
                .param("embeddingInputHash", target.embeddingInputHash())
                .update();
        if (updated != 1) {
            throw catalogChanged();
        }
    }

    private static SynTenPdfEmbeddingOperationSummary summary(
            SynTenPdfEmbeddingState initialState, int alreadyEmbeddedChunks, boolean noOp) {
        return new SynTenPdfEmbeddingOperationSummary(
                SynTenPdfCatalogPlanner.ACCEPTED_CATALOG_FINGERPRINT,
                initialState,
                SynTenPdfCatalogPlanner.EXPECTED_CHUNK_COUNT,
                alreadyEmbeddedChunks,
                noOp);
    }

    private static IllegalStateException catalogChanged() {
        return new IllegalStateException("Persisted SynTen PDF catalog changed after embedding preparation.");
    }

    private static Instant instant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record LockedChunk(
            UUID chunkId,
            UUID tenantId,
            UUID documentVersionId,
            String sourceFormat,
            String embeddingInputHash,
            String embeddingModelId,
            Integer embeddingDimensions,
            Boolean embeddingNormalized,
            Instant embeddedAt,
            Integer vectorDimensions,
            Double vectorNorm) {

        SynTenPdfEmbeddingMetadata embeddingMetadata() {
            return new SynTenPdfEmbeddingMetadata(
                    embeddingModelId,
                    embeddingDimensions,
                    embeddingNormalized,
                    embeddedAt,
                    vectorDimensions,
                    vectorNorm);
        }
    }
}
