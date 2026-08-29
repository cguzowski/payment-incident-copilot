package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertIngestionServiceTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-22T07:15:00Z");
    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");

    @Mock
    private IncidentRepository incidentRepository;

    @Captor
    private ArgumentCaptor<Incident> incidentCaptor;

    @Test
    void validAlertIsPersistedAsANewAuthorizationDeclineRateIncident() {
        when(incidentRepository.insertIfAbsent(any(Incident.class))).thenReturn(true);
        AlertIngestionService service =
                new AlertIngestionService(incidentRepository, Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));

        AlertIngestionResult result = service.ingest(new IngestAlertCommand(
                TENANT_ID,
                "alert-auth-decline-001",
                IncidentSeverity.CRITICAL,
                Instant.parse("2026-08-22T07:14:00Z"),
                "Authorization decline rate above threshold",
                "Synthetic authorization declines exceeded 25% for five minutes."));

        verify(incidentRepository).insertIfAbsent(incidentCaptor.capture());
        Incident persisted = incidentCaptor.getValue();
        assertThat(persisted.tenantId()).isEqualTo(TENANT_ID);
        assertThat(persisted.incidentType()).isEqualTo(IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE);
        assertThat(persisted.status()).isEqualTo(IncidentStatus.NEW);
        assertThat(persisted.receivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(result.created()).isTrue();
        assertThat(result.incident()).isEqualTo(persisted);
    }

    @Test
    void duplicateTenantAndExternalAlertIdReturnsTheExistingIncident() {
        Incident existing = new Incident(
                UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f"),
                TENANT_ID,
                "alert-auth-decline-001",
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                IncidentSeverity.CRITICAL,
                IncidentStatus.NEW,
                "Authorization decline rate above threshold",
                "Synthetic authorization declines exceeded 25% for five minutes.",
                Instant.parse("2026-08-22T07:14:00Z"),
                Instant.parse("2026-08-22T07:14:10Z"));
        when(incidentRepository.insertIfAbsent(any(Incident.class))).thenReturn(false);
        when(incidentRepository.findByTenantIdAndExternalAlertId(TENANT_ID, "alert-auth-decline-001"))
                .thenReturn(Optional.of(existing));
        AlertIngestionService service =
                new AlertIngestionService(incidentRepository, Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));

        AlertIngestionResult result = service.ingest(new IngestAlertCommand(
                TENANT_ID,
                "alert-auth-decline-001",
                IncidentSeverity.HIGH,
                Instant.parse("2026-08-22T07:14:30Z"),
                "Duplicate alert payload",
                "This repeated delivery must not replace the first incident."));

        assertThat(result.created()).isFalse();
        assertThat(result.incident()).isEqualTo(existing);
        verify(incidentRepository).insertIfAbsent(any(Incident.class));
    }
}
