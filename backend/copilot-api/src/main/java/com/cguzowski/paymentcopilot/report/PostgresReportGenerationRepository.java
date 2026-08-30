package com.cguzowski.paymentcopilot.report;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
class PostgresReportGenerationRepository
        implements ReportGenerationRepository, ReviewCandidateProvider, ReportTimelineSnapshotProvider {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    PostgresReportGenerationRepository(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean insertStarted(ReportGenerationAttempt attempt) {
        if (attempt.status() != ReportGenerationStatus.STARTED) {
            throw new IllegalArgumentException("Only started report attempts can be inserted.");
        }
        terminalizeInterruptedAttempt(attempt);
        return jdbcClient
                        .sql("""
                        INSERT INTO report_generation_attempt (
                            id, tenant_id, investigation_id, incident_id,
                            investigation_correlation_id, requested_by, status,
                            requested_at, model_id, temperature, max_output_tokens,
                            prompt_version, prompt_hash, schema_version, schema_hash,
                            latest_evidence_id, applicable_evidence_id, retrieval_id
                        )
                        SELECT :id, :tenantId, :investigationId, :incidentId,
                               :correlationId, :requestedBy, 'STARTED',
                               :requestedAt, :modelId, :temperature, :maxOutputTokens,
                               :promptVersion, :promptHash, :schemaVersion, :schemaHash,
                               :latestEvidenceId, :applicableEvidenceId, :retrievalId
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM report_generation_attempt
                            WHERE tenant_id = :tenantId
                              AND investigation_id = :investigationId
                              AND status IN ('STARTED', 'AVAILABLE')
                        )
                        ON CONFLICT DO NOTHING
                        """)
                        .param("id", attempt.attemptId())
                        .param("tenantId", attempt.tenantId())
                        .param("investigationId", attempt.investigationId())
                        .param("incidentId", attempt.incidentId())
                        .param("correlationId", attempt.correlationId())
                        .param("requestedBy", attempt.requestedBy())
                        .param("requestedAt", utc(attempt.requestedAt()))
                        .param("modelId", attempt.modelId())
                        .param("temperature", attempt.temperature())
                        .param("maxOutputTokens", attempt.maxOutputTokens())
                        .param("promptVersion", attempt.promptVersion())
                        .param("promptHash", attempt.promptHash())
                        .param("schemaVersion", attempt.schemaVersion())
                        .param("schemaHash", attempt.schemaHash())
                        .param("latestEvidenceId", attempt.latestEvidenceId())
                        .param("applicableEvidenceId", nullable(Types.OTHER, attempt.applicableEvidenceId()))
                        .param("retrievalId", attempt.retrievalId())
                        .update()
                == 1;
    }

    private void terminalizeInterruptedAttempt(ReportGenerationAttempt retry) {
        jdbcClient
                .sql("""
                        UPDATE report_generation_attempt
                        SET status = 'UNAVAILABLE',
                            completed_at = :completedAt,
                            status_detail = 'Generation was interrupted before completion.'
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                          AND status = 'STARTED'
                          AND requested_at < :interruptedBefore
                        """)
                .param("completedAt", utc(retry.requestedAt()))
                .param("tenantId", retry.tenantId())
                .param("investigationId", retry.investigationId())
                .param("interruptedBefore", utc(retry.requestedAt().minus(10, ChronoUnit.MINUTES)))
                .update();
    }

    @Override
    public boolean completeFailure(ReportGenerationAttempt attempt) {
        requireTerminal(attempt, false);
        return jdbcClient
                        .sql("""
                        UPDATE report_generation_attempt
                        SET status = :status,
                            completed_at = :completedAt,
                            provider_request_id = :providerRequestId,
                            status_detail = :statusDetail
                        WHERE tenant_id = :tenantId
                          AND id = :attemptId
                          AND status = 'STARTED'
                        """)
                        .param("status", attempt.status().name())
                        .param("completedAt", utc(attempt.completedAt()))
                        .param("providerRequestId", nullable(Types.VARCHAR, attempt.providerRequestId()))
                        .param("statusDetail", nullable(Types.VARCHAR, attempt.statusDetail()))
                        .param("tenantId", attempt.tenantId())
                        .param("attemptId", attempt.attemptId())
                        .update()
                == 1;
    }

    @Override
    public boolean completeAvailable(ReportGenerationAttempt attempt) {
        requireTerminal(attempt, true);
        int updated = jdbcClient
                .sql("""
                        UPDATE report_generation_attempt
                        SET status = 'AVAILABLE',
                            completed_at = :completedAt,
                            provider_request_id = :providerRequestId,
                            disposition = :disposition,
                            report_content = CAST(:reportContent AS JSONB)
                        WHERE tenant_id = :tenantId
                          AND id = :attemptId
                          AND status = 'STARTED'
                        """)
                .param("completedAt", utc(attempt.completedAt()))
                .param("providerRequestId", nullable(Types.VARCHAR, attempt.providerRequestId()))
                .param("disposition", attempt.report().disposition().name())
                .param("reportContent", writeReport(attempt.report()))
                .param("tenantId", attempt.tenantId())
                .param("attemptId", attempt.attemptId())
                .update();
        if (updated != 1) {
            return false;
        }
        insertClaims(attempt);
        return true;
    }

    @Override
    public List<ReportGenerationAttempt> findAll(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT id, tenant_id, investigation_id, incident_id,
                               investigation_correlation_id, requested_by, status,
                               requested_at, completed_at, model_id, temperature,
                               max_output_tokens, prompt_version, prompt_hash,
                               schema_version, schema_hash, latest_evidence_id,
                               applicable_evidence_id, retrieval_id,
                               provider_request_id, status_detail, report_content
                        FROM report_generation_attempt
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                        ORDER BY requested_at DESC, id DESC
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query(this::mapAttempt)
                .list();
    }

    @Override
    public Optional<ReviewCandidate> findReviewCandidate(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT tenant_id, investigation_id, incident_id,
                               investigation_correlation_id, id
                        FROM report_generation_attempt
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                          AND status = 'AVAILABLE'
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query((resultSet, rowNumber) -> new ReviewCandidate(
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getObject("investigation_id", UUID.class),
                        resultSet.getObject("incident_id", UUID.class),
                        resultSet.getObject("investigation_correlation_id", UUID.class),
                        resultSet.getObject("id", UUID.class)))
                .optional();
    }

    @Override
    public List<ReportTimelineSnapshot> findTimelineSnapshots(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT id, investigation_correlation_id, requested_by,
                               status, requested_at, completed_at, model_id,
                               prompt_version, schema_version, disposition
                        FROM report_generation_attempt
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query((resultSet, rowNumber) -> new ReportTimelineSnapshot(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("investigation_correlation_id", UUID.class),
                        resultSet.getObject("requested_by", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getTimestamp("requested_at").toInstant(),
                        instantOrNull(resultSet, "completed_at"),
                        resultSet.getString("model_id"),
                        resultSet.getString("prompt_version"),
                        resultSet.getString("schema_version"),
                        resultSet.getString("disposition")))
                .list();
    }

    private ReportGenerationAttempt mapAttempt(ResultSet resultSet, int rowNumber) throws SQLException {
        String content = resultSet.getString("report_content");
        return new ReportGenerationAttempt(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("investigation_id", UUID.class),
                resultSet.getObject("incident_id", UUID.class),
                resultSet.getObject("investigation_correlation_id", UUID.class),
                resultSet.getObject("requested_by", UUID.class),
                ReportGenerationStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("requested_at").toInstant(),
                instantOrNull(resultSet, "completed_at"),
                resultSet.getString("model_id"),
                resultSet.getInt("temperature"),
                resultSet.getInt("max_output_tokens"),
                resultSet.getString("prompt_version"),
                resultSet.getString("prompt_hash"),
                resultSet.getString("schema_version"),
                resultSet.getString("schema_hash"),
                resultSet.getObject("latest_evidence_id", UUID.class),
                resultSet.getObject("applicable_evidence_id", UUID.class),
                resultSet.getObject("retrieval_id", UUID.class),
                resultSet.getString("provider_request_id"),
                resultSet.getString("status_detail"),
                content == null ? null : readReport(content));
    }

    private void insertClaims(ReportGenerationAttempt attempt) {
        for (PersistedClaim persistedClaim : claims(attempt.report())) {
            ReportClaim claim = persistedClaim.claim();
            jdbcClient
                    .sql("""
                            INSERT INTO report_claim (
                                tenant_id, attempt_id, claim_type, claim_ordinal, statement
                            ) VALUES (
                                :tenantId, :attemptId, :claimType, :ordinal, :statement
                            )
                            """)
                    .param("tenantId", attempt.tenantId())
                    .param("attemptId", attempt.attemptId())
                    .param("claimType", persistedClaim.type())
                    .param("ordinal", persistedClaim.ordinal())
                    .param("statement", claim.statement())
                    .update();
            for (UUID evidenceId : claim.evidenceIds()) {
                jdbcClient
                        .sql("""
                                INSERT INTO report_claim_evidence_reference (
                                    tenant_id, attempt_id, claim_type, claim_ordinal, evidence_id
                                ) VALUES (
                                    :tenantId, :attemptId, :claimType, :ordinal, :evidenceId
                                )
                                """)
                        .param("tenantId", attempt.tenantId())
                        .param("attemptId", attempt.attemptId())
                        .param("claimType", persistedClaim.type())
                        .param("ordinal", persistedClaim.ordinal())
                        .param("evidenceId", evidenceId)
                        .update();
            }
            for (UUID chunkId : claim.knowledgeChunkIds()) {
                jdbcClient
                        .sql("""
                                INSERT INTO report_claim_knowledge_reference (
                                    tenant_id, attempt_id, claim_type, claim_ordinal,
                                    retrieval_id, chunk_id
                                ) VALUES (
                                    :tenantId, :attemptId, :claimType, :ordinal,
                                    :retrievalId, :chunkId
                                )
                                """)
                        .param("tenantId", attempt.tenantId())
                        .param("attemptId", attempt.attemptId())
                        .param("claimType", persistedClaim.type())
                        .param("ordinal", persistedClaim.ordinal())
                        .param("retrievalId", attempt.retrievalId())
                        .param("chunkId", chunkId)
                        .update();
            }
        }
    }

    private static List<PersistedClaim> claims(ReportDocument report) {
        List<PersistedClaim> claims = new ArrayList<>();
        claims.add(new PersistedClaim("SUMMARY", 0, report.summary()));
        addAll(claims, "OBSERVATION", report.observations());
        addAll(claims, "INFERENCE", report.inferences());
        addOptional(claims, "PROBABLE_CAUSE", report.probableCause());
        claims.add(new PersistedClaim(
                "CONFIDENCE",
                0,
                new ReportClaim(
                        report.confidence().rationale(), report.confidence().evidenceIds(), List.of())));
        addOptional(claims, "RECOMMENDATION", report.recommendation());
        addAll(claims, "CONTRADICTION", report.contradictions());
        return claims;
    }

    private static void addAll(List<PersistedClaim> target, String type, List<ReportClaim> claims) {
        for (int index = 0; index < claims.size(); index++) {
            target.add(new PersistedClaim(type, index, claims.get(index)));
        }
    }

    private static void addOptional(List<PersistedClaim> target, String type, ReportClaim claim) {
        if (claim != null) {
            target.add(new PersistedClaim(type, 0, claim));
        }
    }

    private void requireTerminal(ReportGenerationAttempt attempt, boolean available) {
        if (!attempt.status().isTerminal()
                || attempt.completedAt() == null
                || (attempt.status() == ReportGenerationStatus.AVAILABLE) != available
                || (available && attempt.report() == null)
                || (!available && attempt.report() != null)) {
            throw new IllegalArgumentException("The report attempt is not a valid terminal outcome.");
        }
    }

    private String writeReport(ReportDocument report) {
        try {
            return jsonMapper.writeValueAsString(report);
        } catch (JacksonException exception) {
            throw new IllegalStateException("The validated report could not be serialized.", exception);
        }
    }

    private ReportDocument readReport(String content) {
        try {
            return jsonMapper.readValue(content, ReportDocument.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted report content is invalid.", exception);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static SqlParameterValue nullable(int type, Object value) {
        return new SqlParameterValue(type, value);
    }

    private static Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record PersistedClaim(String type, int ordinal, ReportClaim claim) {}
}
