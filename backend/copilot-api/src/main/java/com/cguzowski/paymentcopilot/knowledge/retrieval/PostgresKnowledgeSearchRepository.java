package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeSourceFormat;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PostgresKnowledgeSearchRepository implements KnowledgeSearchRepository {

    private final JdbcClient jdbcClient;

    PostgresKnowledgeSearchRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<KnowledgeSearchCandidate> search(KnowledgeSearchRequest request) {
        String vector = request.queryEmbedding() == null ? null : vectorLiteral(request.queryEmbedding());
        return jdbcClient
                .sql("""
                        WITH query_terms AS (
                            SELECT CASE
                                WHEN CARDINALITY(TSVECTOR_TO_ARRAY(
                                    TO_TSVECTOR('english', :queryText)
                                )) = 0 THEN NULL
                                ELSE TO_TSQUERY(
                                    'english',
                                    ARRAY_TO_STRING(
                                        TSVECTOR_TO_ARRAY(TO_TSVECTOR('english', :queryText)),
                                        ' | '
                                    )
                                )
                            END AS query
                        ),
                        eligible AS (
                            SELECT c.id AS chunk_id,
                                   c.tenant_id,
                                   c.document_version_id,
                                   c.chunk_ordinal,
                                   c.section_path,
                                   c.raw_content,
                                   c.source_start_line,
                                   c.source_end_line,
                                   c.source_start_page,
                                   c.source_end_page,
                                   c.source_start_block,
                                   c.source_end_block,
                                   c.embedding,
                                   c.embedding_model_id,
                                   c.embedding_dimensions,
                                   d.document_id,
                                   d.document_type,
                                   d.title AS document_title,
                                   d.document_version,
                                   d.incident_family,
                                   d.applies_to,
                                   d.approval_status,
                                   d.approved_by,
                                   d.approved_at,
                                   d.effective_at,
                                   d.source_name,
                                   d.source_format,
                                   d.pdf_artifact_hash,
                                   SETWEIGHT(TO_TSVECTOR('english', d.title), 'A')
                                   || SETWEIGHT(TO_TSVECTOR('english', d.applies_to), 'A')
                                   || c.search_vector AS combined_search
                            FROM knowledge_chunk c
                            JOIN knowledge_document_version d
                              ON d.tenant_id = c.tenant_id
                             AND d.id = c.document_version_id
                            WHERE c.tenant_id = :tenantId
                              AND d.approval_status = 'APPROVED'
                              AND d.effective_at <= :effectiveAt
                              AND d.incident_family = :incidentFamily
                        ),
                        lexical_scored AS (
                            SELECT e.*,
                                   TS_RANK_CD(e.combined_search, q.query) AS lexical_rank
                            FROM eligible e
                            CROSS JOIN query_terms q
                            WHERE q.query IS NOT NULL
                        ),
                        lexical_ranked AS (
                            SELECT lexical_scored.*,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY document_type
                                       ORDER BY lexical_rank DESC,
                                                document_id,
                                                document_version,
                                                chunk_ordinal
                                   ) AS lexical_position
                            FROM lexical_scored
                            WHERE lexical_rank > :minimumLexicalRank
                        ),
                        lexical_limited AS (
                            SELECT *
                            FROM lexical_ranked
                            WHERE lexical_position <= :candidateDepth
                        ),
                        vector_scored AS (
                            SELECT e.*,
                                   1 - (e.embedding <=> CAST(CAST(:queryVector AS TEXT) AS vector))
                                       AS vector_similarity
                            FROM eligible e
                            WHERE CAST(:queryVector AS TEXT) IS NOT NULL
                              AND e.embedding IS NOT NULL
                              AND e.embedding_model_id = :embeddingModelId
                              AND e.embedding_dimensions = :embeddingDimensions
                        ),
                        vector_ranked AS (
                            SELECT vector_scored.*,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY document_type
                                       ORDER BY vector_similarity DESC,
                                                document_id,
                                                document_version,
                                                chunk_ordinal
                                   ) AS vector_position
                            FROM vector_scored
                            WHERE vector_similarity >= :minimumVectorSimilarity
                        ),
                        vector_limited AS (
                            SELECT *
                            FROM vector_ranked
                            WHERE vector_position <= :candidateDepth
                        ),
                        fused AS (
                            SELECT COALESCE(l.chunk_id, v.chunk_id) AS chunk_id,
                                   l.lexical_rank,
                                   CAST(l.lexical_position AS INTEGER) AS lexical_position,
                                   CAST(v.vector_similarity AS REAL) AS vector_similarity,
                                   CAST(v.vector_position AS INTEGER) AS vector_position,
                                   COALESCE(1.0 / (:rrfK + l.lexical_position), 0.0)
                                   + COALESCE(1.0 / (:rrfK + v.vector_position), 0.0)
                                       AS fused_score
                            FROM lexical_limited l
                            FULL OUTER JOIN vector_limited v ON v.chunk_id = l.chunk_id
                        )
                        SELECT e.tenant_id,
                               e.chunk_id,
                               e.chunk_ordinal,
                               e.document_version_id,
                               e.document_id,
                               e.document_type,
                               e.document_title,
                               e.document_version,
                               e.incident_family,
                               e.applies_to,
                               e.section_path,
                               e.raw_content,
                               e.source_name,
                               e.source_format,
                               e.pdf_artifact_hash,
                               e.source_start_line,
                               e.source_end_line,
                               e.source_start_page,
                               e.source_end_page,
                               e.source_start_block,
                               e.source_end_block,
                               e.approval_status,
                               e.approved_by,
                               e.approved_at,
                               e.effective_at,
                               f.lexical_rank,
                               f.lexical_position,
                               f.vector_similarity,
                               f.vector_position,
                               f.fused_score
                        FROM fused f
                        JOIN eligible e ON e.chunk_id = f.chunk_id
                        ORDER BY f.fused_score DESC,
                                 LEAST(
                                     COALESCE(f.lexical_position, 2147483647),
                                     COALESCE(f.vector_position, 2147483647)
                                 ),
                                 e.document_type,
                                 e.document_id,
                                 e.document_version,
                                 e.chunk_ordinal
                        """)
                .param("queryText", request.queryText())
                .param("tenantId", request.tenantId())
                .param("effectiveAt", OffsetDateTime.ofInstant(request.effectiveAt(), ZoneOffset.UTC))
                .param("incidentFamily", request.incidentFamily())
                .param("minimumLexicalRank", request.minimumLexicalRank())
                .param("queryVector", new SqlParameterValue(Types.VARCHAR, vector))
                .param("embeddingModelId", request.embeddingModelId())
                .param("embeddingDimensions", request.embeddingDimensions())
                .param("minimumVectorSimilarity", request.minimumVectorSimilarity())
                .param("candidateDepth", request.candidateDepth())
                .param("rrfK", request.rrfK())
                .query(this::mapCandidate)
                .list();
    }

    private KnowledgeSearchCandidate mapCandidate(ResultSet resultSet, int rowNumber) throws SQLException {
        return new KnowledgeSearchCandidate(
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("chunk_id", UUID.class),
                resultSet.getInt("chunk_ordinal"),
                resultSet.getObject("document_version_id", UUID.class),
                resultSet.getObject("document_id", UUID.class),
                KnowledgeDocumentType.valueOf(resultSet.getString("document_type")),
                resultSet.getString("document_title"),
                resultSet.getString("document_version"),
                resultSet.getString("incident_family"),
                resultSet.getString("applies_to"),
                resultSet.getString("section_path"),
                resultSet.getString("raw_content"),
                resultSet.getString("source_name"),
                KnowledgeSourceFormat.valueOf(resultSet.getString("source_format")),
                resultSet.getString("pdf_artifact_hash"),
                nullableInteger(resultSet, "source_start_line"),
                nullableInteger(resultSet, "source_end_line"),
                nullableInteger(resultSet, "source_start_page"),
                nullableInteger(resultSet, "source_end_page"),
                nullableInteger(resultSet, "source_start_block"),
                nullableInteger(resultSet, "source_end_block"),
                KnowledgeApprovalStatus.valueOf(resultSet.getString("approval_status")),
                resultSet.getObject("approved_by", UUID.class),
                resultSet.getTimestamp("approved_at").toInstant(),
                resultSet.getTimestamp("effective_at").toInstant(),
                nullableFloat(resultSet, "lexical_rank"),
                nullableInteger(resultSet, "lexical_position"),
                nullableFloat(resultSet, "vector_similarity"),
                nullableInteger(resultSet, "vector_position"),
                resultSet.getDouble("fused_score"));
    }

    private static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                value.append(',');
            }
            value.append(Float.toString(vector[index]));
        }
        return value.append(']').toString();
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
