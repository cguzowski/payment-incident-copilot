package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class EvidenceCollectionServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID EVIDENCE_ID = UUID.fromString("a8bab9d4-dccc-4e70-acfe-174ac63a3b12");
    private static final UUID TOOL_CALL_ID = UUID.fromString("21fdc56b-267a-4cb5-81b9-50f092e0ef35");
    private static final Instant NOW = Instant.parse("2026-08-28T10:00:02Z");
    private static final Instant RETRIEVED_AT = Instant.parse("2026-08-28T10:00:01Z");
    private static final EvidenceCollectionContext CONTEXT = new EvidenceCollectionContext(
            TENANT_ID,
            INVESTIGATION_ID,
            CORRELATION_ID,
            "alert-auth-decline-001");

    @Test
    void persistsStartedAttemptBeforeCallingSourceAndThenCompletesIt() {
        EvidenceCollectionPersistenceService persistence = mock(EvidenceCollectionPersistenceService.class);
        ServiceErrorEvidenceGateway gateway = mock(ServiceErrorEvidenceGateway.class);
        EvidenceIdentifierGenerator identifiers = mock(EvidenceIdentifierGenerator.class);
        when(persistence.findContext(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(CONTEXT));
        when(identifiers.next()).thenReturn(EVIDENCE_ID, TOOL_CALL_ID);
        when(gateway.collect(CONTEXT, TOOL_CALL_ID)).thenReturn(availableResult());
        when(persistence.complete(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        EvidenceCollectionService service = service(persistence, gateway, identifiers);

        EvidenceCollectionResponse response = service.collect(TENANT_ID, INVESTIGATION_ID);

        EvidenceCollectionAttempt started = EvidenceCollectionAttempt.started(
                EVIDENCE_ID,
                TENANT_ID,
                INVESTIGATION_ID,
                TOOL_CALL_ID,
                CORRELATION_ID,
                "synthetic-observability",
                "getRecentServiceErrors",
                "alert-auth-decline-001",
                NOW,
                "service-errors/v1");
        EvidenceCollectionAttempt completed = started.complete(
                EvidenceCollectionStatus.AVAILABLE,
                RETRIEVED_AT,
                NOW,
                availableResult().content(),
                null);
        InOrder order = inOrder(persistence, gateway);
        order.verify(persistence).findContext(TENANT_ID, INVESTIGATION_ID);
        order.verify(persistence).insertStarted(started);
        order.verify(gateway).collect(CONTEXT, TOOL_CALL_ID);
        order.verify(persistence).complete(completed);
        assertThat(response).isEqualTo(EvidenceCollectionResponse.from(completed));
    }

    @Test
    void doesNotCallSourceOrPersistForCrossTenantInvestigation() {
        EvidenceCollectionPersistenceService persistence = mock(EvidenceCollectionPersistenceService.class);
        ServiceErrorEvidenceGateway gateway = mock(ServiceErrorEvidenceGateway.class);
        EvidenceIdentifierGenerator identifiers = mock(EvidenceIdentifierGenerator.class);
        when(persistence.findContext(OTHER_TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(persistence, gateway, identifiers)
                        .collect(OTHER_TENANT_ID, INVESTIGATION_ID))
                .isInstanceOf(InvestigationNotFoundException.class);
        verify(persistence, never()).insertStarted(org.mockito.ArgumentMatchers.any());
        verify(gateway, never()).collect(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retainsStartedAttemptWhenCollectionIsInterrupted() {
        EvidenceCollectionPersistenceService persistence = mock(EvidenceCollectionPersistenceService.class);
        ServiceErrorEvidenceGateway gateway = mock(ServiceErrorEvidenceGateway.class);
        EvidenceIdentifierGenerator identifiers = mock(EvidenceIdentifierGenerator.class);
        when(persistence.findContext(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(CONTEXT));
        when(identifiers.next()).thenReturn(EVIDENCE_ID, TOOL_CALL_ID);
        when(gateway.collect(CONTEXT, TOOL_CALL_ID)).thenThrow(new IllegalStateException("interrupted"));

        assertThatThrownBy(() -> service(persistence, gateway, identifiers)
                        .collect(TENANT_ID, INVESTIGATION_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(persistence).insertStarted(org.mockito.ArgumentMatchers.any());
        verify(persistence, never()).complete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsEveryAttemptNewestFirstFromTenantScopedHistory() {
        EvidenceCollectionPersistenceService persistence = mock(EvidenceCollectionPersistenceService.class);
        when(persistence.findContext(TENANT_ID, INVESTIGATION_ID)).thenReturn(Optional.of(CONTEXT));
        EvidenceCollectionAttempt older = EvidenceCollectionAttempt.started(
                UUID.randomUUID(), TENANT_ID, INVESTIGATION_ID, UUID.randomUUID(), CORRELATION_ID,
                "synthetic-observability", "getRecentServiceErrors", "alert-auth-decline-001",
                Instant.parse("2026-08-28T09:59:00Z"), "service-errors/v1");
        EvidenceCollectionAttempt newer = EvidenceCollectionAttempt.started(
                UUID.randomUUID(), TENANT_ID, INVESTIGATION_ID, UUID.randomUUID(), CORRELATION_ID,
                "synthetic-observability", "getRecentServiceErrors", "alert-auth-decline-001",
                Instant.parse("2026-08-28T10:00:00Z"), "service-errors/v1");
        when(persistence.findAll(TENANT_ID, INVESTIGATION_ID)).thenReturn(List.of(newer, older));
        EvidenceCollectionService service = service(
                persistence,
                mock(ServiceErrorEvidenceGateway.class),
                mock(EvidenceIdentifierGenerator.class));

        assertThat(service.history(TENANT_ID, INVESTIGATION_ID))
                .containsExactly(
                        EvidenceCollectionResponse.from(newer),
                        EvidenceCollectionResponse.from(older));
    }

    private static EvidenceCollectionService service(
            EvidenceCollectionPersistenceService persistence,
            ServiceErrorEvidenceGateway gateway,
            EvidenceIdentifierGenerator identifiers) {
        return new EvidenceCollectionService(
                persistence,
                gateway,
                identifiers,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static EvidenceSourceResult availableResult() {
        return new EvidenceSourceResult(
                "synthetic-observability",
                "getRecentServiceErrors",
                RETRIEVED_AT,
                CORRELATION_ID,
                TOOL_CALL_ID,
                EvidenceCollectionStatus.AVAILABLE,
                null,
                "service-errors/v1",
                new ServiceErrorEvidenceContent(
                        "payment-authorization-service",
                        Instant.parse("2026-08-28T09:55:00Z"),
                        Instant.parse("2026-08-28T10:00:00Z"),
                        List.of(new ServiceErrorObservation(
                                "service-error-001",
                                Instant.parse("2026-08-28T09:58:00Z"),
                                "UPSTREAM_TIMEOUT",
                                14))));
    }
}
