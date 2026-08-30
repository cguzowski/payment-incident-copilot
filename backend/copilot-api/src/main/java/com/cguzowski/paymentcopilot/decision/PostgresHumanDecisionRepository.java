package com.cguzowski.paymentcopilot.decision;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PostgresHumanDecisionRepository implements HumanDecisionRepository, DecisionTimelineSnapshotProvider {

    private final JdbcClient jdbcClient;

    PostgresHumanDecisionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean insertIfAbsent(HumanDecision decision) {
        return jdbcClient
                        .sql("""
                        INSERT INTO human_decision (
                            id, tenant_id, investigation_id, incident_id,
                            investigation_correlation_id, report_attempt_id,
                            decided_by, outcome, reason, decided_at
                        ) VALUES (
                            :id, :tenantId, :investigationId, :incidentId,
                            :correlationId, :reportAttemptId,
                            :decidedBy, :outcome, :reason, :decidedAt
                        )
                        ON CONFLICT DO NOTHING
                        """)
                        .param("id", decision.decisionId())
                        .param("tenantId", decision.tenantId())
                        .param("investigationId", decision.investigationId())
                        .param("incidentId", decision.incidentId())
                        .param("correlationId", decision.investigationCorrelationId())
                        .param("reportAttemptId", decision.reportAttemptId())
                        .param("decidedBy", decision.decidedBy())
                        .param("outcome", decision.outcome().name())
                        .param("reason", decision.reason())
                        .param("decidedAt", OffsetDateTime.ofInstant(decision.decidedAt(), ZoneOffset.UTC))
                        .update()
                == 1;
    }

    @Override
    public Optional<HumanDecision> find(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT id, tenant_id, investigation_id, incident_id,
                               investigation_correlation_id, report_attempt_id,
                               decided_by, outcome, reason, decided_at
                        FROM human_decision
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query(PostgresHumanDecisionRepository::mapDecision)
                .optional();
    }

    @Override
    public List<DecisionTimelineSnapshot> findTimelineSnapshots(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT id, investigation_correlation_id, report_attempt_id,
                               decided_by, outcome, reason, decided_at
                        FROM human_decision
                        WHERE tenant_id = :tenantId
                          AND investigation_id = :investigationId
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query((resultSet, rowNumber) -> new DecisionTimelineSnapshot(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("investigation_correlation_id", UUID.class),
                        resultSet.getObject("report_attempt_id", UUID.class),
                        resultSet.getObject("decided_by", UUID.class),
                        resultSet.getString("outcome"),
                        resultSet.getString("reason"),
                        resultSet.getTimestamp("decided_at").toInstant()))
                .list();
    }

    private static HumanDecision mapDecision(ResultSet resultSet, int rowNumber) throws SQLException {
        return new HumanDecision(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getObject("investigation_id", UUID.class),
                resultSet.getObject("incident_id", UUID.class),
                resultSet.getObject("investigation_correlation_id", UUID.class),
                resultSet.getObject("report_attempt_id", UUID.class),
                resultSet.getObject("decided_by", UUID.class),
                DecisionOutcome.valueOf(resultSet.getString("outcome")),
                resultSet.getString("reason"),
                resultSet.getTimestamp("decided_at").toInstant());
    }
}
