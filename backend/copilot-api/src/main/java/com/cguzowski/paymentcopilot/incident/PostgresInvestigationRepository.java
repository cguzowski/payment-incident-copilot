package com.cguzowski.paymentcopilot.incident;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class PostgresInvestigationRepository
        implements InvestigationRepository,
                InvestigationSnapshotProvider,
                EvidenceCollectionContextProvider,
                ReportInvestigationSnapshotProvider,
                ReportLifecyclePort {

    private final JdbcClient jdbcClient;

    PostgresInvestigationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<IncidentStatus> lockIncidentStatus(UUID tenantId, UUID incidentId) {
        return jdbcClient
                .sql("""
                        SELECT status
                        FROM incident
                        WHERE tenant_id = :tenantId AND id = :incidentId
                        FOR UPDATE
                        """)
                .param("tenantId", tenantId)
                .param("incidentId", incidentId)
                .query(String.class)
                .optional()
                .map(IncidentStatus::valueOf);
    }

    @Override
    public void insert(Investigation investigation) {
        jdbcClient
                .sql("""
                        INSERT INTO investigation (
                            id, tenant_id, incident_id, started_by, started_at, correlation_id
                        ) VALUES (
                            :id, :tenantId, :incidentId, :startedBy, :startedAt, :correlationId
                        )
                        """)
                .param("id", investigation.id())
                .param("tenantId", investigation.tenantId())
                .param("incidentId", investigation.incidentId())
                .param("startedBy", investigation.startedBy())
                .param("startedAt", OffsetDateTime.ofInstant(investigation.startedAt(), ZoneOffset.UTC))
                .param("correlationId", investigation.correlationId())
                .update();
    }

    @Override
    public boolean transitionIncidentToInvestigating(UUID tenantId, UUID incidentId) {
        return jdbcClient
                        .sql("""
                        UPDATE incident
                        SET status = 'INVESTIGATING'
                        WHERE tenant_id = :tenantId AND id = :incidentId AND status = 'NEW'
                        """)
                        .param("tenantId", tenantId)
                        .param("incidentId", incidentId)
                        .update()
                == 1;
    }

    @Override
    public Optional<InvestigationView> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId) {
        return find("investigation.incident_id = :lookupId", tenantId, incidentId);
    }

    @Override
    public Optional<InvestigationView> findByTenantIdAndInvestigationId(UUID tenantId, UUID investigationId) {
        return find("investigation.id = :lookupId", tenantId, investigationId);
    }

    @Override
    public Optional<InvestigationSnapshot> findKnowledgeRetrievalSnapshot(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT investigation.tenant_id,
                               investigation.id AS investigation_id,
                               investigation.correlation_id,
                               incident.incident_type,
                               incident.summary,
                               incident.description
                        FROM investigation
                        JOIN incident
                          ON incident.tenant_id = investigation.tenant_id
                         AND incident.id = investigation.incident_id
                        WHERE investigation.tenant_id = :tenantId
                          AND investigation.id = :investigationId
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query((resultSet, rowNumber) -> new InvestigationSnapshot(
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getObject("investigation_id", UUID.class),
                        resultSet.getObject("correlation_id", UUID.class),
                        resultSet.getString("incident_type"),
                        resultSet.getString("summary"),
                        resultSet.getString("description")))
                .optional();
    }

    @Override
    public Optional<EvidenceCollectionContext> findEvidenceCollectionContext(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT investigation.tenant_id,
                               investigation.id AS investigation_id,
                               investigation.correlation_id,
                               incident.external_alert_id
                        FROM investigation
                        JOIN incident
                          ON incident.tenant_id = investigation.tenant_id
                         AND incident.id = investigation.incident_id
                        WHERE investigation.tenant_id = :tenantId
                          AND investigation.id = :investigationId
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query((resultSet, rowNumber) -> new EvidenceCollectionContext(
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getObject("investigation_id", UUID.class),
                        resultSet.getObject("correlation_id", UUID.class),
                        resultSet.getString("external_alert_id")))
                .optional();
    }

    @Override
    public Optional<ReportInvestigationSnapshot> findForReport(UUID tenantId, UUID investigationId) {
        return jdbcClient
                .sql("""
                        SELECT investigation.tenant_id,
                               investigation.id AS investigation_id,
                               investigation.incident_id,
                               investigation.correlation_id,
                               incident.status,
                               incident.incident_type,
                               incident.summary,
                               incident.description
                        FROM investigation
                        JOIN incident
                          ON incident.tenant_id = investigation.tenant_id
                         AND incident.id = investigation.incident_id
                        WHERE investigation.tenant_id = :tenantId
                          AND investigation.id = :investigationId
                        """)
                .param("tenantId", tenantId)
                .param("investigationId", investigationId)
                .query((resultSet, rowNumber) -> new ReportInvestigationSnapshot(
                        resultSet.getObject("tenant_id", UUID.class),
                        resultSet.getObject("investigation_id", UUID.class),
                        resultSet.getObject("incident_id", UUID.class),
                        resultSet.getObject("correlation_id", UUID.class),
                        resultSet.getString("status"),
                        resultSet.getString("incident_type"),
                        resultSet.getString("summary"),
                        resultSet.getString("description")))
                .optional();
    }

    @Override
    public boolean transitionToAwaitingReview(UUID tenantId, UUID incidentId) {
        return jdbcClient
                        .sql("""
                        UPDATE incident
                        SET status = 'AWAITING_REVIEW'
                        WHERE tenant_id = :tenantId
                          AND id = :incidentId
                          AND status = 'INVESTIGATING'
                        """)
                        .param("tenantId", tenantId)
                        .param("incidentId", incidentId)
                        .update()
                == 1;
    }

    private Optional<InvestigationView> find(String predicate, UUID tenantId, UUID lookupId) {
        return jdbcClient
                .sql("""
                        SELECT investigation.id, investigation.tenant_id,
                               investigation.incident_id, investigation.started_by,
                               investigation.started_at, investigation.correlation_id,
                               incident.status AS incident_status
                        FROM investigation
                        JOIN incident
                          ON incident.tenant_id = investigation.tenant_id
                         AND incident.id = investigation.incident_id
                        WHERE investigation.tenant_id = :tenantId AND %s
                        """.formatted(predicate))
                .param("tenantId", tenantId)
                .param("lookupId", lookupId)
                .query((resultSet, rowNumber) -> new InvestigationView(
                        new Investigation(
                                resultSet.getObject("id", UUID.class),
                                resultSet.getObject("tenant_id", UUID.class),
                                resultSet.getObject("incident_id", UUID.class),
                                resultSet.getObject("started_by", UUID.class),
                                resultSet.getTimestamp("started_at").toInstant(),
                                resultSet.getObject("correlation_id", UUID.class)),
                        IncidentStatus.valueOf(resultSet.getString("incident_status"))))
                .optional();
    }
}
