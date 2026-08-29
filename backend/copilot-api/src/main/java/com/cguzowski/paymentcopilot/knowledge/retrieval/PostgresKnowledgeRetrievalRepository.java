package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
class PostgresKnowledgeRetrievalRepository implements KnowledgeRetrievalRepository {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    PostgresKnowledgeRetrievalRepository(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void insertStarted(KnowledgeRetrievalAttempt attempt) {
        if (attempt.status() != KnowledgeRetrievalStatus.STARTED) {
            throw new IllegalArgumentException("Only started knowledge retrievals can be inserted.");
        }
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_retrieval_attempt (
                            id, tenant_id, investigation_id, investigation_correlation_id,
                            status, requested_at, query_text, query_template_version,
                            contributing_evidence_ids, embedding_model_id,
                            embedding_dimensions, metadata_filters, ranking_version,
                            rrf_k, candidate_depth, minimum_lexical_rank,
                            minimum_vector_similarity
                        ) VALUES (
                            :id, :tenantId, :investigationId, :correlationId,
                            'STARTED', :requestedAt, :queryText, :queryTemplateVersion,
                            CAST(:evidenceIds AS UUID[]), :embeddingModelId,
                            :embeddingDimensions, CAST(:metadataFilters AS JSONB), :rankingVersion,
                            :rrfK, :candidateDepth, :minimumLexicalRank,
                            :minimumVectorSimilarity
                        )
                        """)
                .param("id", attempt.retrievalId())
                .param("tenantId", attempt.tenantId())
                .param("investigationId", attempt.investigationId())
                .param("correlationId", attempt.investigationCorrelationId())
                .param("requestedAt", utc(attempt.requestedAt()))
                .param("queryText", attempt.queryText())
                .param("queryTemplateVersion", attempt.queryTemplateVersion())
                .param("evidenceIds", uuidArrayLiteral(attempt.contributingEvidenceIds()))
                .param("embeddingModelId", attempt.embeddingModelId())
                .param("embeddingDimensions", attempt.embeddingDimensions())
                .param("metadataFilters", writeFilters(attempt.metadataFilters()))
                .param("rankingVersion", attempt.rankingVersion())
                .param("rrfK", attempt.rrfK())
                .param("candidateDepth", attempt.candidateDepth())
                .param("minimumLexicalRank", attempt.minimumLexicalRank())
                .param("minimumVectorSimilarity", attempt.minimumVectorSimilarity())
                .update();
    }

    @Override
    public boolean complete(KnowledgeRetrievalAttempt attempt) {
        if (!attempt.status().isTerminal() || attempt.completedAt() == null) {
            throw new IllegalArgumentException("Knowledge retrieval completion requires a terminal status.");
        }
        int updated = jdbcClient
                .sql("""
                        UPDATE knowledge_retrieval_attempt
                        SET status = :status,
                            completed_at = :completedAt,
                            status_detail = :statusDetail
                        WHERE tenant_id = :tenantId
                          AND id = :retrievalId
                          AND status = 'STARTED'
                        """)
                .param("status", attempt.status().name())
                .param("completedAt", utc(attempt.completedAt()))
                .param("statusDetail", nullable(Types.VARCHAR, attempt.statusDetail()))
                .param("tenantId", attempt.tenantId())
                .param("retrievalId", attempt.retrievalId())
                .update();
        if (updated != 1) {
            return false;
        }
        for (KnowledgeRetrievalResult result : attempt.results()) {
            insertResult(attempt, result);
        }
        return true;
    }

    @Override
    public List<KnowledgeRetrievalAttempt> findAll(UUID tenantId, UUID investigationId) {
        List<KnowledgeRetrievalAttempt> attempts = jdbcClient
                .sql("""
                        SELECT id, tenant_id, investigation_id,
                               investigation_correlation_id, status, requested_at,
                               completed_at, query_text, query_template_version,
                               contributing_evidence_ids, embedding_model_id,
                               embedding_dimensions, metadata_filters, ranking_version,
                               rrf_k, candidate_depth, minimum_lexical_rank,
                               minimum_vector_similarity, status_detail
                        FROM knowledge_retrieval_attempt
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                        ORDER BY requested_at DESC, id DESC
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query(this::mapAttemptWithoutResults)
                .list();
        return attempts.stream()
                .map(attempt -> withResults(attempt, findResults(tenantId, attempt.retrievalId())))
                .toList();
    }

    private KnowledgeRetrievalAttempt mapAttemptWithoutResults(ResultSet resultSet, int rowNumber) throws SQLException {
        java.sql.Array evidenceArray = resultSet.getArray("contributing_evidence_ids");
        List<UUID> evidenceIds = evidenceArray == null ? List.of() : Arrays.asList((UUID[]) evidenceArray.getArray());
        return new KnowledgeRetrievalAttempt(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("investigation_id", UUID.class),
                resultSet.getObject("investigation_correlation_id", UUID.class),
                KnowledgeRetrievalStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("requested_at").toInstant(),
                instantOrNull(resultSet, "completed_at"),
                resultSet.getString("query_text"),
                resultSet.getString("query_template_version"),
                evidenceIds,
                resultSet.getString("embedding_model_id"),
                resultSet.getInt("embedding_dimensions"),
                readFilters(resultSet.getString("metadata_filters")),
                resultSet.getString("ranking_version"),
                resultSet.getInt("rrf_k"),
                resultSet.getInt("candidate_depth"),
                resultSet.getFloat("minimum_lexical_rank"),
                resultSet.getFloat("minimum_vector_similarity"),
                resultSet.getString("status_detail"),
                List.of());
    }

    private List<KnowledgeRetrievalResult> findResults(UUID tenantId, UUID retrievalId) {
        return jdbcClient
                .sql("""
                        SELECT chunk_id, document_version_id, document_id,
                               selected_position, lexical_rank, lexical_position,
                               vector_similarity, vector_distance, vector_position,
                               fused_position, fused_score, document_type,
                               document_title, document_version, applies_to,
                               section_path, raw_content, source_start_line,
                               source_end_line, approval_status, approved_by,
                               approved_at, effective_at
                        FROM knowledge_retrieval_result
                        WHERE tenant_id = :tenantId
                          AND retrieval_id = :retrievalId
                        ORDER BY selected_position
                        """)
                .param("tenantId", tenantId)
                .param("retrievalId", retrievalId)
                .query(this::mapResult)
                .list();
    }

    private KnowledgeRetrievalResult mapResult(ResultSet resultSet, int rowNumber) throws SQLException {
        return new KnowledgeRetrievalResult(
                resultSet.getObject("chunk_id", UUID.class),
                resultSet.getObject("document_version_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                resultSet.getInt("selected_position"),
                nullableFloat(resultSet, "lexical_rank"),
                nullableInteger(resultSet, "lexical_position"),
                nullableFloat(resultSet, "vector_similarity"),
                nullableFloat(resultSet, "vector_distance"),
                nullableInteger(resultSet, "vector_position"),
                resultSet.getInt("fused_position"),
                resultSet.getDouble("fused_score"),
                KnowledgeDocumentType.valueOf(resultSet.getString("document_type")),
                resultSet.getString("document_title"),
                resultSet.getString("document_version"),
                resultSet.getString("applies_to"),
                resultSet.getString("section_path"),
                resultSet.getString("raw_content"),
                resultSet.getInt("source_start_line"),
                resultSet.getInt("source_end_line"),
                KnowledgeApprovalStatus.valueOf(resultSet.getString("approval_status")),
                resultSet.getObject("approved_by", UUID.class),
                resultSet.getTimestamp("approved_at").toInstant(),
                resultSet.getTimestamp("effective_at").toInstant());
    }

    private void insertResult(KnowledgeRetrievalAttempt attempt, KnowledgeRetrievalResult result) {
        jdbcClient
                .sql("""
                        INSERT INTO knowledge_retrieval_result (
                            retrieval_id, tenant_id, chunk_id, document_version_id,
                            document_id, selected_position, lexical_rank,
                            lexical_position, vector_similarity, vector_distance,
                            vector_position, fused_position, fused_score, document_type,
                            document_title, document_version, applies_to, section_path,
                            raw_content, source_start_line, source_end_line,
                            approval_status, approved_by, approved_at, effective_at
                        ) VALUES (
                            :retrievalId, :tenantId, :chunkId, :documentVersionId,
                            :documentId, :selectedPosition, :lexicalRank,
                            :lexicalPosition, :vectorSimilarity, :vectorDistance,
                            :vectorPosition, :fusedPosition, :fusedScore, :documentType,
                            :documentTitle, :documentVersion, :appliesTo, :sectionPath,
                            :rawContent, :sourceStartLine, :sourceEndLine,
                            :approvalStatus, :approvedBy, :approvedAt, :effectiveAt
                        )
                        """)
                .param("retrievalId", attempt.retrievalId())
                .param("tenantId", attempt.tenantId())
                .param("chunkId", result.chunkId())
                .param("documentVersionId", result.documentVersionId())
                .param("documentId", result.documentId())
                .param("selectedPosition", result.selectedPosition())
                .param("lexicalRank", nullable(Types.REAL, result.lexicalRank()))
                .param("lexicalPosition", nullable(Types.INTEGER, result.lexicalPosition()))
                .param("vectorSimilarity", nullable(Types.REAL, result.vectorSimilarity()))
                .param("vectorDistance", nullable(Types.REAL, result.vectorDistance()))
                .param("vectorPosition", nullable(Types.INTEGER, result.vectorPosition()))
                .param("fusedPosition", result.fusedPosition())
                .param("fusedScore", result.fusedScore())
                .param("documentType", result.documentType().name())
                .param("documentTitle", result.documentTitle())
                .param("documentVersion", result.documentVersion())
                .param("appliesTo", result.appliesTo())
                .param("sectionPath", result.sectionPath())
                .param("rawContent", result.rawContent())
                .param("sourceStartLine", result.sourceStartLine())
                .param("sourceEndLine", result.sourceEndLine())
                .param("approvalStatus", result.approvalStatus().name())
                .param("approvedBy", result.approvedBy())
                .param("approvedAt", utc(result.approvedAt()))
                .param("effectiveAt", utc(result.effectiveAt()))
                .update();
    }

    private static KnowledgeRetrievalAttempt withResults(
            KnowledgeRetrievalAttempt attempt, List<KnowledgeRetrievalResult> results) {
        return new KnowledgeRetrievalAttempt(
                attempt.retrievalId(),
                attempt.tenantId(),
                attempt.investigationId(),
                attempt.investigationCorrelationId(),
                attempt.status(),
                attempt.requestedAt(),
                attempt.completedAt(),
                attempt.queryText(),
                attempt.queryTemplateVersion(),
                attempt.contributingEvidenceIds(),
                attempt.embeddingModelId(),
                attempt.embeddingDimensions(),
                attempt.metadataFilters(),
                attempt.rankingVersion(),
                attempt.rrfK(),
                attempt.candidateDepth(),
                attempt.minimumLexicalRank(),
                attempt.minimumVectorSimilarity(),
                attempt.statusDetail(),
                results);
    }

    private String writeFilters(KnowledgeMetadataFilters filters) {
        try {
            return jsonMapper.writeValueAsString(filters);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Knowledge metadata filters could not be serialized.", exception);
        }
    }

    private KnowledgeMetadataFilters readFilters(String json) {
        try {
            return jsonMapper.readValue(json, KnowledgeMetadataFilters.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted knowledge metadata filters are invalid.", exception);
        }
    }

    private static String uuidArrayLiteral(List<UUID> identifiers) {
        return identifiers.stream()
                .map(UUID::toString)
                .reduce((left, right) -> left + "," + right)
                .map(joined -> "{" + joined + "}")
                .orElse("{}");
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static SqlParameterValue nullable(int sqlType, Object value) {
        return new SqlParameterValue(sqlType, value);
    }

    private static Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Float nullableFloat(ResultSet resultSet, String column) throws SQLException {
        float value = resultSet.getFloat(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
