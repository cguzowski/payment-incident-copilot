package com.cguzowski.paymentcopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContext;
import com.cguzowski.paymentcopilot.incident.EvidenceCollectionContextProvider;
import com.cguzowski.paymentcopilot.incident.InvestigationSnapshot;
import com.cguzowski.paymentcopilot.incident.InvestigationSnapshotProvider;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class EvidencePersistencePostgresIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("payment_copilot")
            .withUsername("payment_copilot")
            .withPassword("test_only_password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private EvidenceCollectionRepository evidenceCollectionRepository;

    @Autowired
    private EvidenceCollectionContextProvider contextProvider;

    @Autowired
    private InvestigationSnapshotProvider investigationSnapshots;

    @Autowired
    private EvidenceSnapshotProvider evidenceSnapshots;

    @BeforeEach
    void setUpInvestigation() {
        jdbcClient.sql("DELETE FROM evidence_collection_attempt").update();
        jdbcClient.sql("DELETE FROM investigation").update();
        jdbcClient.sql("DELETE FROM incident").update();
        insertIncidentAndInvestigation();
    }

    @Test
    void persistsEvidenceAttemptWithTenantAndProvenance() {
        UUID evidenceId = UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12");
        UUID toolCallId = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");

        insertStartedAttempt(evidenceId, TENANT_ID, CORRELATION_ID, toolCallId, "2026-08-28T10:00:00Z");

        Map<String, Object> persisted =
                jdbcClient.sql("""
                        SELECT id, tenant_id, investigation_id, tool_call_id,
                               investigation_correlation_id, source_system, source_tool,
                               scenario_reference, status, requested_at,
                               content_schema_version, content, status_detail
                        FROM evidence_collection_attempt
                        WHERE id = :evidenceId
                        """).param("evidenceId", evidenceId).query().singleRow();

        assertThat(persisted.get("tenant_id")).isEqualTo(TENANT_ID);
        assertThat(persisted.get("investigation_id")).isEqualTo(INVESTIGATION_ID);
        assertThat(persisted.get("tool_call_id")).isEqualTo(toolCallId);
        assertThat(persisted.get("investigation_correlation_id")).isEqualTo(CORRELATION_ID);
        assertThat(persisted.get("source_system")).isEqualTo("synthetic-observability");
        assertThat(persisted.get("source_tool")).isEqualTo("getRecentServiceErrors");
        assertThat(persisted.get("scenario_reference")).isEqualTo("alert-auth-decline-001");
        assertThat(persisted.get("status")).isEqualTo("STARTED");
        assertThat(persisted.get("content_schema_version")).isEqualTo("service-errors/v1");
        assertThat(persisted.get("content")).isNull();
        assertThat(persisted.get("status_detail")).isNull();
    }

    @Test
    void preventsCrossTenantOrCorrelationEvidenceAssociation() {
        assertThatThrownBy(() -> insertStartedAttempt(
                        UUID.randomUUID(), OTHER_TENANT_ID, CORRELATION_ID, UUID.randomUUID(), "2026-08-28T10:00:00Z"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> insertStartedAttempt(
                        UUID.randomUUID(), TENANT_ID, UUID.randomUUID(), UUID.randomUUID(), "2026-08-28T10:00:00Z"))
                .isInstanceOf(RuntimeException.class);

        assertThat(attemptCount()).isZero();
    }

    @Test
    void preservesEveryRetryAttemptInNewestFirstOrder() {
        UUID first = UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12");
        UUID second = UUID.fromString("67d4709a-a5dc-4356-8351-af86878c2e2d");
        insertStartedAttempt(first, TENANT_ID, CORRELATION_ID, UUID.randomUUID(), "2026-08-28T10:00:00Z");
        insertStartedAttempt(second, TENANT_ID, CORRELATION_ID, UUID.randomUUID(), "2026-08-28T10:01:00Z");

        List<UUID> ids = jdbcClient
                .sql("""
                        SELECT id
                        FROM evidence_collection_attempt
                        WHERE tenant_id = :tenantId AND investigation_id = :investigationId
                        ORDER BY requested_at DESC, id DESC
                        """)
                .param("tenantId", TENANT_ID)
                .param("investigationId", INVESTIGATION_ID)
                .query(UUID.class)
                .list();

        assertThat(ids).containsExactly(second, first);
    }

    @Test
    void updatesOnlyTheMatchingStartedAttempt() {
        UUID evidenceId = UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12");
        insertStartedAttempt(evidenceId, TENANT_ID, CORRELATION_ID, UUID.randomUUID(), "2026-08-28T10:00:00Z");

        int updated = jdbcClient
                .sql("""
                        UPDATE evidence_collection_attempt
                        SET status = 'UNAVAILABLE',
                            retrieved_at = TIMESTAMPTZ '2026-08-28 10:00:01Z',
                            completed_at = TIMESTAMPTZ '2026-08-28 10:00:02Z',
                            status_detail = 'Synthetic source unavailable.'
                        WHERE tenant_id = :tenantId AND id = :evidenceId AND status = 'STARTED'
                        """)
                .param("tenantId", TENANT_ID)
                .param("evidenceId", evidenceId)
                .update();
        int repeated = jdbcClient
                .sql("""
                        UPDATE evidence_collection_attempt
                        SET status = 'TIMED_OUT', completed_at = TIMESTAMPTZ '2026-08-28 10:00:03Z'
                        WHERE tenant_id = :tenantId AND id = :evidenceId AND status = 'STARTED'
                        """)
                .param("tenantId", TENANT_ID)
                .param("evidenceId", evidenceId)
                .update();

        assertThat(updated).isOne();
        assertThat(repeated).isZero();
        assertThat(jdbcClient
                        .sql("SELECT status FROM evidence_collection_attempt WHERE id = :id")
                        .param("id", evidenceId)
                        .query(String.class)
                        .single())
                .isEqualTo("UNAVAILABLE");
    }

    @Test
    void findsOnlyTenantOwnedInvestigationContext() {
        assertThat(contextProvider.findEvidenceCollectionContext(TENANT_ID, INVESTIGATION_ID))
                .contains(new EvidenceCollectionContext(
                        TENANT_ID, INVESTIGATION_ID, CORRELATION_ID, "alert-auth-decline-001"));
        assertThat(contextProvider.findEvidenceCollectionContext(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
        assertThat(investigationSnapshots.findKnowledgeRetrievalSnapshot(TENANT_ID, INVESTIGATION_ID))
                .contains(new InvestigationSnapshot(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        CORRELATION_ID,
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Authorization decline rate above threshold",
                        "Synthetic authorization decline incident."));
        assertThat(investigationSnapshots.findKnowledgeRetrievalSnapshot(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
    }

    @Test
    void publishesLatestStatusAndNewestEarlierApplicableNormalizedEvidence() {
        EvidenceCollectionAttempt available = startedAttempt(
                UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12"),
                UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35"),
                "2026-08-28T10:00:00Z");
        EvidenceCollectionAttempt unavailable = startedAttempt(
                UUID.fromString("67d4709a-a5dc-4356-8351-af86878c2e2d"),
                UUID.fromString("1dc5f9e9-864c-4619-9b1f-960a160697c3"),
                "2026-08-28T10:01:00Z");
        evidenceCollectionRepository.insertStarted(available);
        evidenceCollectionRepository.insertStarted(unavailable);
        ServiceErrorEvidenceContent content = new ServiceErrorEvidenceContent(
                "payment-authorization-service",
                java.time.Instant.parse("2026-08-28T09:55:00Z"),
                java.time.Instant.parse("2026-08-28T10:00:00Z"),
                List.of(
                        new ServiceErrorObservation(
                                "service-error-001",
                                java.time.Instant.parse("2026-08-28T09:58:00Z"),
                                "UPSTREAM_TIMEOUT",
                                14),
                        new ServiceErrorObservation(
                                "service-error-002",
                                java.time.Instant.parse("2026-08-28T09:59:00Z"),
                                "UPSTREAM_TIMEOUT",
                                3)));
        evidenceCollectionRepository.complete(available.complete(
                EvidenceCollectionStatus.AVAILABLE,
                java.time.Instant.parse("2026-08-28T10:00:01Z"),
                java.time.Instant.parse("2026-08-28T10:00:02Z"),
                content,
                null));
        evidenceCollectionRepository.complete(unavailable.complete(
                EvidenceCollectionStatus.UNAVAILABLE,
                null,
                java.time.Instant.parse("2026-08-28T10:01:02Z"),
                null,
                "Synthetic source unavailable."));

        assertThat(evidenceSnapshots.findByTenantIdAndInvestigationId(TENANT_ID, INVESTIGATION_ID))
                .contains(new EvidenceSnapshot(
                        unavailable.evidenceId(),
                        "UNAVAILABLE",
                        available.evidenceId(),
                        "payment-authorization-service",
                        List.of(new EvidenceErrorCount("UPSTREAM_TIMEOUT", 17))));
        assertThat(evidenceSnapshots.findByTenantIdAndInvestigationId(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
    }

    @Test
    void roundTripsNormalizedTerminalEvidenceNewestFirst() {
        EvidenceCollectionAttempt first = startedAttempt(
                UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12"),
                UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35"),
                "2026-08-28T10:00:00Z");
        EvidenceCollectionAttempt second = startedAttempt(
                UUID.fromString("67d4709a-a5dc-4356-8351-af86878c2e2d"),
                UUID.fromString("1dc5f9e9-864c-4619-9b1f-960a160697c3"),
                "2026-08-28T10:01:00Z");
        evidenceCollectionRepository.insertStarted(first);
        evidenceCollectionRepository.insertStarted(second);
        ServiceErrorEvidenceContent content = new ServiceErrorEvidenceContent(
                "payment-authorization-service",
                java.time.Instant.parse("2026-08-28T09:55:00Z"),
                java.time.Instant.parse("2026-08-28T10:00:00Z"),
                List.of(new ServiceErrorObservation(
                        "service-error-001", java.time.Instant.parse("2026-08-28T09:58:00Z"), "UPSTREAM_TIMEOUT", 14)));
        EvidenceCollectionAttempt completed = first.complete(
                EvidenceCollectionStatus.AVAILABLE,
                java.time.Instant.parse("2026-08-28T10:00:01Z"),
                java.time.Instant.parse("2026-08-28T10:00:02Z"),
                content,
                null);

        assertThat(evidenceCollectionRepository.complete(completed)).isTrue();
        assertThat(evidenceCollectionRepository.complete(completed)).isFalse();
        assertThat(evidenceCollectionRepository.findAll(TENANT_ID, INVESTIGATION_ID))
                .containsExactly(second, completed);
        assertThat(evidenceCollectionRepository.findAll(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
    }

    private void insertIncidentAndInvestigation() {
        jdbcClient
                .sql("""
                        INSERT INTO incident (
                            id, tenant_id, external_alert_id, incident_type, severity, status,
                            summary, description, occurred_at, received_at
                        ) VALUES (
                            :id, :tenantId, 'alert-auth-decline-001',
                            'AUTHORIZATION_DECLINE_RATE_SPIKE', 'HIGH', 'INVESTIGATING',
                            'Authorization decline rate above threshold',
                            'Synthetic authorization decline incident.',
                            TIMESTAMPTZ '2026-08-22 07:14:00Z',
                            TIMESTAMPTZ '2026-08-22 07:15:00Z'
                        )
                        """)
                .param("id", INCIDENT_ID)
                .param("tenantId", TENANT_ID)
                .update();
        jdbcClient
                .sql("""
                        INSERT INTO investigation (
                            id, tenant_id, incident_id, started_by, started_at, correlation_id
                        ) VALUES (
                            :id, :tenantId, :incidentId, :startedBy,
                            TIMESTAMPTZ '2026-08-22 07:16:00Z', :correlationId
                        )
                        """)
                .param("id", INVESTIGATION_ID)
                .param("tenantId", TENANT_ID)
                .param("incidentId", INCIDENT_ID)
                .param("startedBy", UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"))
                .param("correlationId", CORRELATION_ID)
                .update();
    }

    private void insertStartedAttempt(
            UUID evidenceId, UUID tenantId, UUID correlationId, UUID toolCallId, String requestedAt) {
        jdbcClient
                .sql("""
                        INSERT INTO evidence_collection_attempt (
                            id, tenant_id, investigation_id, tool_call_id,
                            investigation_correlation_id, source_system, source_tool,
                            scenario_reference, status, requested_at,
                            content_schema_version
                        ) VALUES (
                            :id, :tenantId, :investigationId, :toolCallId,
                            :correlationId, 'synthetic-observability',
                            'getRecentServiceErrors', 'alert-auth-decline-001',
                            'STARTED', CAST(:requestedAt AS TIMESTAMPTZ),
                            'service-errors/v1'
                        )
                        """)
                .param("id", evidenceId)
                .param("tenantId", tenantId)
                .param("investigationId", INVESTIGATION_ID)
                .param("toolCallId", toolCallId)
                .param("correlationId", correlationId)
                .param("requestedAt", requestedAt)
                .update();
    }

    private int attemptCount() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM evidence_collection_attempt")
                .query(Integer.class)
                .single();
    }

    private static EvidenceCollectionAttempt startedAttempt(UUID evidenceId, UUID toolCallId, String requestedAt) {
        return EvidenceCollectionAttempt.started(
                evidenceId,
                TENANT_ID,
                INVESTIGATION_ID,
                toolCallId,
                CORRELATION_ID,
                "synthetic-observability",
                "getRecentServiceErrors",
                "alert-auth-decline-001",
                java.time.Instant.parse(requestedAt),
                "service-errors/v1");
    }
}
