package com.cguzowski.paymentcopilot.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestigationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INCIDENT_ID = UUID.fromString("f4749ecb-49b0-4277-a140-cb69485b082f");
    private static final UUID INVESTIGATION_ID = UUID.fromString("a012c9cb-85a6-4d77-9703-3b53377b56c3");
    private static final UUID CORRELATION_ID = UUID.fromString("a5d978b5-34c7-42da-9076-22f8e5169315");
    private static final UUID OPERATOR_ID = UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1");
    private static final Instant STARTED_AT = Instant.parse("2026-08-27T18:30:00Z");

    @Mock
    private InvestigationRepository investigationRepository;

    @Mock
    private InvestigationIdentifierGenerator identifierGenerator;

    @Test
    void createsInvestigationAndTransitionsNewIncident() {
        when(investigationRepository.lockIncidentStatus(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.of(IncidentStatus.NEW));
        when(identifierGenerator.next()).thenReturn(INVESTIGATION_ID, CORRELATION_ID);
        when(investigationRepository.transitionIncidentToInvestigating(TENANT_ID, INCIDENT_ID))
                .thenReturn(true);
        InvestigationService service = service();

        InvestigationStartResult result = service.start(TENANT_ID, INCIDENT_ID, OPERATOR_ID);

        assertThat(result.created()).isTrue();
        assertThat(result.response()).isEqualTo(new InvestigationResponse(
                INVESTIGATION_ID,
                INCIDENT_ID,
                IncidentStatus.INVESTIGATING,
                OPERATOR_ID,
                STARTED_AT));
        verify(investigationRepository).insert(new Investigation(
                INVESTIGATION_ID,
                TENANT_ID,
                INCIDENT_ID,
                OPERATOR_ID,
                STARTED_AT,
                CORRELATION_ID));
    }

    @Test
    void returnsExistingInvestigationForRepeatedStart() {
        Investigation existing = investigation();
        when(investigationRepository.lockIncidentStatus(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.of(IncidentStatus.INVESTIGATING));
        when(investigationRepository.findByTenantIdAndIncidentId(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.of(new InvestigationView(existing, IncidentStatus.INVESTIGATING)));
        InvestigationService service = service();

        InvestigationStartResult result = service.start(TENANT_ID, INCIDENT_ID, OPERATOR_ID);

        assertThat(result.created()).isFalse();
        assertThat(result.response()).isEqualTo(InvestigationResponse.from(
                new InvestigationView(existing, IncidentStatus.INVESTIGATING)));
        verify(investigationRepository, never()).insert(existing);
    }

    @Test
    void returnsIncidentNotFoundWithoutTenantLeakage() {
        when(investigationRepository.lockIncidentStatus(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().start(TENANT_ID, INCIDENT_ID, OPERATOR_ID))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void returnsConflictWithoutMutationForInconsistentState() {
        when(investigationRepository.lockIncidentStatus(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.of(IncidentStatus.INVESTIGATING));
        when(investigationRepository.findByTenantIdAndIncidentId(TENANT_ID, INCIDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().start(TENANT_ID, INCIDENT_ID, OPERATOR_ID))
                .isInstanceOf(InvestigationConflictException.class);
        verify(investigationRepository, never()).transitionIncidentToInvestigating(TENANT_ID, INCIDENT_ID);
    }

    private InvestigationService service() {
        return new InvestigationService(
                investigationRepository,
                identifierGenerator,
                Clock.fixed(STARTED_AT, ZoneOffset.UTC));
    }

    private static Investigation investigation() {
        return new Investigation(
                INVESTIGATION_ID,
                TENANT_ID,
                INCIDENT_ID,
                OPERATOR_ID,
                STARTED_AT,
                CORRELATION_ID);
    }
}
