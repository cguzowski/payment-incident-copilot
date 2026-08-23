package com.cguzowski.paymentcopilot.incident;

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
class PostgresIncidentRepository implements IncidentRepository {

    private static final String INCIDENT_COLUMNS = """
            id, tenant_id, external_alert_id, incident_type, severity, status,
            summary, description, occurred_at, received_at
            """;

    private final JdbcClient jdbcClient;

    PostgresIncidentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean insertIfAbsent(Incident incident) {
        int inserted = jdbcClient.sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity, status,
                            summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, :externalAlertId, :incidentType, :severity, :status,
                            :title, :description, :detectedAt, :receivedAt
                        )
                        ON CONFLICT (tenant_id, external_alert_id) DO NOTHING
                        """)
                .param("id", incident.id())
                .param("tenantId", incident.tenantId())
                .param("externalAlertId", incident.externalAlertId())
                .param("incidentType", incident.incidentType().name())
                .param("severity", incident.severity().name())
                .param("status", incident.status().name())
                .param("title", incident.title())
                .param("description", incident.description())
                .param("detectedAt", OffsetDateTime.ofInstant(incident.detectedAt(), ZoneOffset.UTC))
                .param("receivedAt", OffsetDateTime.ofInstant(incident.receivedAt(), ZoneOffset.UTC))
                .update();
        return inserted == 1;
    }

    @Override
    public Optional<Incident> findByTenantIdAndExternalAlertId(UUID tenantId, String externalAlertId) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM incident
                        WHERE tenant_id = :tenantId AND external_alert_id = :externalAlertId
                        """.formatted(INCIDENT_COLUMNS))
                .param("tenantId", tenantId)
                .param("externalAlertId", externalAlertId)
                .query(PostgresIncidentRepository::mapIncident)
                .optional();
    }

    @Override
    public Optional<Incident> findByTenantIdAndIncidentId(UUID tenantId, UUID incidentId) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM incident
                        WHERE tenant_id = :tenantId AND id = :incidentId
                        """.formatted(INCIDENT_COLUMNS))
                .param("tenantId", tenantId)
                .param("incidentId", incidentId)
                .query(PostgresIncidentRepository::mapIncident)
                .optional();
    }

    @Override
    public List<Incident> findQueueByTenantId(UUID tenantId) {
        return jdbcClient.sql("""
                        SELECT %s
                        FROM incident
                        WHERE tenant_id = :tenantId AND status = 'NEW'
                        ORDER BY received_at DESC
                        """.formatted(INCIDENT_COLUMNS))
                .param("tenantId", tenantId)
                .query(PostgresIncidentRepository::mapIncident)
                .list();
    }

    private static Incident mapIncident(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Incident(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                resultSet.getString("external_alert_id"),
                IncidentType.valueOf(resultSet.getString("incident_type")),
                IncidentSeverity.valueOf(resultSet.getString("severity")),
                IncidentStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("summary"),
                resultSet.getString("description"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getTimestamp("received_at").toInstant());
    }
}
