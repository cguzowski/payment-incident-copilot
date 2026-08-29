package com.cguzowski.paymentcopilot.evidence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
class PostgresEvidenceCollectionRepository implements EvidenceCollectionRepository, EvidenceSnapshotProvider {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    PostgresEvidenceCollectionRepository(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<EvidenceSnapshot> findByTenantIdAndInvestigationId(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT latest.id AS latest_evidence_id,
                               latest.status AS latest_evidence_status,
                               applicable.id AS applicable_evidence_id,
                               applicable.content AS applicable_evidence_content
                        FROM (
                            SELECT id, status
                            FROM evidence_collection_attempt
                            WHERE tenant_id = :tenantId
                              AND investigation_id = :investigationId
                              AND source_tool = 'getRecentServiceErrors'
                            ORDER BY requested_at DESC, id DESC
                            LIMIT 1
                        ) latest
                        LEFT JOIN LATERAL (
                            SELECT id, content
                            FROM evidence_collection_attempt
                            WHERE tenant_id = :tenantId
                              AND investigation_id = :investigationId
                              AND source_tool = 'getRecentServiceErrors'
                              AND status IN ('AVAILABLE', 'PARTIAL')
                            ORDER BY requested_at DESC, id DESC
                            LIMIT 1
                        ) applicable ON TRUE
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query(this::mapSnapshot)
                .optional();
    }

    @Override
    public void insertStarted(EvidenceCollectionAttempt attempt) {
        if (attempt.status() != EvidenceCollectionStatus.STARTED) {
            throw new IllegalArgumentException("Only started evidence attempts can be inserted.");
        }
        jdbcClient
                .sql("""
                        INSERT INTO evidence_collection_attempt (
                            id, tenant_id, investigation_id, tool_call_id,
                            investigation_correlation_id, source_system, source_tool,
                            scenario_reference, status, requested_at,
                            content_schema_version
                        ) VALUES (
                            :id, :tenantId, :investigationId, :toolCallId,
                            :correlationId, :sourceSystem, :sourceTool,
                            :scenarioReference, :status, :requestedAt,
                            :contentSchemaVersion
                        )
                        """)
                .param("id", attempt.evidenceId())
                .param("tenantId", attempt.tenantId())
                .param("investigationId", attempt.investigationId())
                .param("toolCallId", attempt.toolCallId())
                .param("correlationId", attempt.investigationCorrelationId())
                .param("sourceSystem", attempt.sourceSystem())
                .param("sourceTool", attempt.sourceTool())
                .param("scenarioReference", attempt.scenarioReference())
                .param("status", attempt.status().name())
                .param("requestedAt", utc(attempt.requestedAt()))
                .param("contentSchemaVersion", attempt.contentSchemaVersion())
                .update();
    }

    @Override
    public boolean complete(EvidenceCollectionAttempt attempt) {
        if (!attempt.status().isTerminal() || attempt.completedAt() == null) {
            throw new IllegalArgumentException("Evidence completion requires a terminal status and timestamp.");
        }
        String contentJson = attempt.content() == null ? null : writeContent(attempt.content());
        return jdbcClient
                        .sql("""
                        UPDATE evidence_collection_attempt
                        SET status = :status,
                            retrieved_at = :retrievedAt,
                            completed_at = :completedAt,
                            content = CAST(:content AS JSONB),
                            status_detail = :statusDetail
                        WHERE tenant_id = :tenantId
                          AND id = :evidenceId
                          AND tool_call_id = :toolCallId
                          AND status = 'STARTED'
                        """)
                        .param("status", attempt.status().name())
                        .param("retrievedAt", nullableUtc(attempt.retrievedAt()))
                        .param("completedAt", utc(attempt.completedAt()))
                        .param("content", new SqlParameterValue(Types.VARCHAR, contentJson))
                        .param("statusDetail", new SqlParameterValue(Types.VARCHAR, attempt.statusDetail()))
                        .param("tenantId", attempt.tenantId())
                        .param("evidenceId", attempt.evidenceId())
                        .param("toolCallId", attempt.toolCallId())
                        .update()
                == 1;
    }

    @Override
    public List<EvidenceCollectionAttempt> findAll(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT id, tenant_id, investigation_id, tool_call_id,
                               investigation_correlation_id, source_system, source_tool,
                               scenario_reference, status, requested_at, retrieved_at,
                               completed_at, content_schema_version, content, status_detail
                        FROM evidence_collection_attempt
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                        ORDER BY requested_at DESC, id DESC
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query(this::mapAttempt)
                .list();
    }

    private EvidenceCollectionAttempt mapAttempt(ResultSet resultSet, int rowNumber) throws SQLException {
        String contentJson = resultSet.getString("content");
        return new EvidenceCollectionAttempt(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("investigation_id", UUID.class),
                resultSet.getObject("tool_call_id", UUID.class),
                resultSet.getObject("investigation_correlation_id", UUID.class),
                resultSet.getString("source_system"),
                resultSet.getString("source_tool"),
                resultSet.getString("scenario_reference"),
                EvidenceCollectionStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("requested_at").toInstant(),
                instantOrNull(resultSet, "retrieved_at"),
                instantOrNull(resultSet, "completed_at"),
                resultSet.getString("content_schema_version"),
                contentJson == null ? null : readContent(contentJson),
                resultSet.getString("status_detail"));
    }

    private EvidenceSnapshot mapSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        UUID applicableAttemptId = resultSet.getObject("applicable_evidence_id", UUID.class);
        String applicableContentJson = resultSet.getString("applicable_evidence_content");
        ServiceErrorEvidenceContent content = applicableContentJson == null ? null : readContent(applicableContentJson);
        Map<String, Integer> countsByCode = new TreeMap<>();
        if (content != null) {
            for (ServiceErrorObservation observation : content.errors()) {
                countsByCode.merge(observation.errorCode(), observation.count(), Integer::sum);
            }
        }
        return new EvidenceSnapshot(
                resultSet.getObject("latest_evidence_id", UUID.class),
                resultSet.getString("latest_evidence_status"),
                applicableAttemptId,
                content == null ? null : content.serviceName(),
                countsByCode.entrySet().stream()
                        .map(entry -> new EvidenceErrorCount(entry.getKey(), entry.getValue()))
                        .toList());
    }

    private String writeContent(ServiceErrorEvidenceContent content) {
        try {
            return jsonMapper.writeValueAsString(content);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Validated evidence content could not be serialized.", exception);
        }
    }

    private ServiceErrorEvidenceContent readContent(String contentJson) {
        try {
            return jsonMapper.readValue(contentJson, ServiceErrorEvidenceContent.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted evidence content is invalid.", exception);
        }
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Object nullableUtc(java.time.Instant instant) {
        return instant == null ? new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, null) : utc(instant);
    }

    private static java.time.Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
