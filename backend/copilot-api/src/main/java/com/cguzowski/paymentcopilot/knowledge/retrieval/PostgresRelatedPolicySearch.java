package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;

/** One bounded expansion over the same eligible catalog; never follows file references. */
final class PostgresRelatedPolicySearch {
    private final JdbcClient jdbc;

    PostgresRelatedPolicySearch(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<KnowledgeSearchCandidate> expand(KnowledgeSearchRequest request, List<KnowledgeSearchCandidate> direct) {
        List<UUID> anchors = direct.stream()
                .filter(c -> c.documentType() == KnowledgeDocumentType.RUNBOOK)
                .map(KnowledgeSearchCandidate::documentVersionId)
                .distinct()
                .limit(4)
                .toList();
        if (anchors.isEmpty()) {
            return direct;
        }
        String vector = request.queryEmbedding() == null
                ? null
                : PostgresKnowledgeSearchRepository.vectorLiteral(request.queryEmbedding());
        List<KnowledgeSearchCandidate> related = jdbc.sql("""
                WITH eligible AS (
                    SELECT d.* FROM knowledge_document_version d
                    WHERE d.tenant_id=:tenantId AND d.incident_family=:family
                      AND d.approval_status='APPROVED' AND d.effective_at<=:effectiveAt
                ), keyed AS (
                    SELECT e.*, COUNT(*) OVER(PARTITION BY document_key) AS key_count
                    FROM eligible e WHERE document_key IS NOT NULL
                      AND relationship_metadata_version='document-relationships/v1'
                ), unambiguous AS (
                    SELECT * FROM keyed WHERE key_count=1
                ), anchors AS (
                    SELECT d.*, a.position AS anchor_position FROM unambiguous d
                    JOIN UNNEST(CAST(:anchors AS UUID[])) WITH ORDINALITY a(id,position) ON a.id=d.id
                    WHERE d.document_type='RUNBOOK'
                ), paths AS (
                    SELECT p.id AS target_id, a.id AS anchor_id, a.document_key AS anchor_key,
                           a.source_artifact_hash AS anchor_hash, a.anchor_position,
                           NULL::uuid AS intermediate_id, NULL::text AS intermediate_key,
                           NULL::text AS intermediate_hash, 1 AS hops
                    FROM anchors a JOIN unambiguous p ON p.document_key=ANY(a.related_document_keys)
                    WHERE p.document_type='POLICY'
                    UNION ALL
                    SELECT p.id, a.id, a.document_key, a.source_artifact_hash, a.anchor_position,
                           b.id, b.document_key, b.source_artifact_hash, 2
                    FROM anchors a JOIN unambiguous b ON b.document_key=ANY(a.related_document_keys)
                    JOIN unambiguous p ON p.document_key=ANY(b.related_document_keys)
                    WHERE b.document_type='RUNBOOK' AND b.id<>a.id AND p.document_type='POLICY'
                ), best_paths AS (
                    SELECT DISTINCT ON(target_id) * FROM paths
                    ORDER BY target_id, anchor_position, hops, intermediate_id
                ), query_terms AS (
                    SELECT CASE WHEN CARDINALITY(TSVECTOR_TO_ARRAY(TO_TSVECTOR('english',:queryText)))=0
                        THEN NULL ELSE TO_TSQUERY('english', ARRAY_TO_STRING(
                        TSVECTOR_TO_ARRAY(TO_TSVECTOR('english',:queryText)), ' | ')) END AS query
                ), scored AS (
                    SELECT c.id AS chunk_id,
                           TS_RANK_CD(SETWEIGHT(TO_TSVECTOR('english',d.title),'A')
                               || SETWEIGHT(TO_TSVECTOR('english',d.applies_to),'A') || c.search_vector,q.query) AS lex,
                           CASE WHEN c.embedding_model_id=:modelId AND c.embedding_dimensions=:dimensions
                               THEN 1-(c.embedding <=> CAST(CAST(:vector AS TEXT) AS vector)) END AS sim
                    FROM eligible d JOIN knowledge_chunk c ON c.document_version_id=d.id AND c.tenant_id=d.tenant_id
                    CROSS JOIN query_terms q WHERE d.document_type='POLICY'
                ), lexical AS (
                    SELECT chunk_id, lex, CAST(ROW_NUMBER() OVER(ORDER BY lex DESC,chunk_id) AS INTEGER) AS pos
                    FROM scored WHERE lex>:minimumLexical
                ), semantic AS (
                    SELECT chunk_id, sim, CAST(ROW_NUMBER() OVER(ORDER BY sim DESC,chunk_id) AS INTEGER) AS pos
                    FROM scored WHERE sim>=:minimumVector
                ), choices AS (
                    SELECT c.id AS chosen_chunk_id, p.*,
                           ROW_NUMBER() OVER(PARTITION BY d.id ORDER BY
                               COALESCE(1.0/(:rrfK+l.pos),0.0)+COALESCE(1.0/(:rrfK+v.pos),0.0) DESC,
                               c.chunk_ordinal) AS choice
                    FROM best_paths p JOIN eligible d ON d.id=p.target_id
                    JOIN knowledge_chunk c ON c.document_version_id=d.id AND c.tenant_id=d.tenant_id
                    LEFT JOIN lexical l ON l.chunk_id=c.id LEFT JOIN semantic v ON v.chunk_id=c.id
                    WHERE l.chunk_id IS NOT NULL OR v.chunk_id IS NOT NULL
                )
                SELECT c.*, c.id AS chunk_id, d.document_id, d.document_type, d.title AS document_title,
                       d.document_version,d.incident_family,d.applies_to,d.source_name,d.source_format,
                       d.pdf_artifact_hash,d.approval_status,d.approved_by,d.approved_at,d.effective_at,
                       d.document_key AS target_key,
                       l.lex AS lexical_rank,l.pos AS lexical_position,
                       CAST(v.sim AS REAL) AS vector_similarity,v.pos AS vector_position,
                       COALESCE(1.0/(:rrfK+l.pos),0.0)+COALESCE(1.0/(:rrfK+v.pos),0.0) AS fused_score,
                       0 AS signal_matches,p.anchor_id,p.anchor_key,p.anchor_hash,p.anchor_position,
                       p.intermediate_id,p.intermediate_key,p.intermediate_hash,p.hops
                FROM choices p JOIN knowledge_chunk c ON c.id=p.chosen_chunk_id
                JOIN eligible d ON d.id=c.document_version_id AND d.tenant_id=c.tenant_id
                LEFT JOIN lexical l ON l.chunk_id=c.id LEFT JOIN semantic v ON v.chunk_id=c.id
                WHERE p.choice=1 ORDER BY p.anchor_position,p.hops,d.document_id,d.document_version,c.chunk_ordinal
                LIMIT 20
                """)
                .param("tenantId", request.tenantId())
                .param("family", request.incidentFamily())
                .param("effectiveAt", OffsetDateTime.ofInstant(request.effectiveAt(), ZoneOffset.UTC))
                .param(
                        "anchors",
                        "{"
                                + String.join(
                                        ",",
                                        anchors.stream().map(UUID::toString).toList()) + "}")
                .param("queryText", request.queryText())
                .param("modelId", request.embeddingModelId())
                .param("dimensions", request.embeddingDimensions())
                .param("vector", new SqlParameterValue(Types.VARCHAR, vector))
                .param("minimumLexical", request.minimumLexicalRank())
                .param("minimumVector", request.minimumVectorSimilarity())
                .param("rrfK", request.rrfK())
                .query((rs, row) -> {
                    KnowledgeSearchCandidate candidate = PostgresKnowledgeSearchRepository.mapCandidate(rs, row);
                    KnowledgeRelationshipEvidence path = new KnowledgeRelationshipEvidence(
                            rs.getObject("anchor_id", UUID.class),
                            rs.getString("anchor_key"),
                            rs.getString("anchor_hash"),
                            rs.getObject("intermediate_id", UUID.class),
                            rs.getString("intermediate_key"),
                            rs.getString("intermediate_hash"),
                            rs.getString("target_key"),
                            rs.getInt("anchor_position"),
                            rs.getInt("hops"),
                            "document-relationships/v1");
                    return candidate.withRanking(candidate.fusedScore(), new KnowledgeRankingEvidence(0, path));
                })
                .list();
        LinkedHashMap<UUID, KnowledgeSearchCandidate> merged = new LinkedHashMap<>();
        direct.forEach(c -> merged.put(c.chunkId(), c));
        for (KnowledgeSearchCandidate linked : related) {
            KnowledgeSearchCandidate original = merged.getOrDefault(linked.chunkId(), linked);
            KnowledgeRelationshipEvidence path = linked.rankingEvidence().relationship();
            double relationshipScore = 10.0 / (3 * path.anchorPosition() + path.hops());
            merged.put(
                    linked.chunkId(),
                    original.withRanking(
                            original.fusedScore() + relationshipScore,
                            new KnowledgeRankingEvidence(
                                    original.rankingEvidence().exactSignalMatches(), path)));
        }
        List<KnowledgeSearchCandidate> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparingDouble(KnowledgeSearchCandidate::fusedScore)
                .reversed()
                .thenComparingInt(c -> Math.min(
                        c.lexicalPosition() == null ? Integer.MAX_VALUE : c.lexicalPosition(),
                        c.vectorPosition() == null ? Integer.MAX_VALUE : c.vectorPosition()))
                .thenComparing(c -> c.documentType().name())
                .thenComparing(c -> c.documentId().toString())
                .thenComparing(KnowledgeSearchCandidate::documentVersion)
                .thenComparingInt(KnowledgeSearchCandidate::chunkOrdinal));
        return List.copyOf(result);
    }
}
