package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentDetailServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("076a18a3-d54f-486a-b3ec-189e1048fd28");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");

    @Mock
    private IncidentRepository incidentRepository;

    @Test
    void returnsIncidentDetailForOwningTenant() {
        Incident incident = incident();
        when(incidentRepository.findByTenantIdAndIncidentId(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.of(incident));
        IncidentDetailService service = new IncidentDetailService(incidentRepository);

        IncidentDetailResponse response = service.getDetail(TENANT_ID, INCIDENT_ID);

        assertThat(response).isEqualTo(IncidentDetailResponse.from(incident));
        verify(incidentRepository).findByTenantIdAndIncidentId(TENANT_ID, INCIDENT_ID);
    }

    @Test
    void returnsNotFoundWhenIncidentDoesNotExist() {
        when(incidentRepository.findByTenantIdAndIncidentId(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.empty());
        IncidentDetailService service = new IncidentDetailService(incidentRepository);

        assertThatThrownBy(() -> service.getDetail(TENANT_ID, INCIDENT_ID))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void returnsNotFoundWhenIncidentBelongsToAnotherTenant() {
        when(incidentRepository.findByTenantIdAndIncidentId(OTHER_TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.empty());
        IncidentDetailService service = new IncidentDetailService(incidentRepository);

        assertThatThrownBy(() -> service.getDetail(OTHER_TENANT_ID, INCIDENT_ID))
                .isInstanceOf(IncidentNotFoundException.class);
        verify(incidentRepository).findByTenantIdAndIncidentId(OTHER_TENANT_ID, INCIDENT_ID);
    }

    private static Incident incident() {
        return new Incident(
                INCIDENT_ID,
                TENANT_ID,
                "alert-auth-decline-001",
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                IncidentSeverity.HIGH,
                IncidentStatus.NEW,
                "Authorization decline rate above threshold",
                "Synthetic authorization declines exceeded 25% for five minutes.",
                Instant.parse("2026-08-22T07:14:00Z"),
                Instant.parse("2026-08-22T07:15:00Z"));
    }
}
