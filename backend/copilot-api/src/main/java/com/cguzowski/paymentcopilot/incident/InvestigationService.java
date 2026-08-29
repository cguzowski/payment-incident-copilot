package com.cguzowski.paymentcopilot.incident;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final InvestigationIdentifierGenerator identifierGenerator;
    private final Clock clock;

    InvestigationService(
            InvestigationRepository investigationRepository,
            InvestigationIdentifierGenerator identifierGenerator,
            Clock clock) {
        this.investigationRepository = investigationRepository;
        this.identifierGenerator = identifierGenerator;
        this.clock = clock;
    }

    @Transactional
    InvestigationStartResult start(UUID tenantId, UUID incidentId, UUID operatorId) {
        IncidentStatus status = investigationRepository
                .lockIncidentStatus(tenantId, incidentId)
                .orElseThrow(IncidentNotFoundException::new);

        if (status == IncidentStatus.INVESTIGATING) {
            InvestigationView existing = investigationRepository
                    .findByTenantIdAndIncidentId(tenantId, incidentId)
                    .orElseThrow(InvestigationConflictException::new);
            return new InvestigationStartResult(InvestigationResponse.from(existing), false);
        }
        if (status != IncidentStatus.NEW) {
            throw new InvestigationConflictException();
        }

        Instant startedAt = Instant.now(clock);
        Investigation investigation = new Investigation(
                identifierGenerator.next(), tenantId, incidentId, operatorId, startedAt, identifierGenerator.next());
        investigationRepository.insert(investigation);
        if (!investigationRepository.transitionIncidentToInvestigating(tenantId, incidentId)) {
            throw new InvestigationConflictException();
        }

        InvestigationView created = new InvestigationView(investigation, IncidentStatus.INVESTIGATING);
        return new InvestigationStartResult(InvestigationResponse.from(created), true);
    }

    @Transactional(readOnly = true)
    InvestigationResponse get(UUID tenantId, UUID investigationId) {
        return investigationRepository
                .findByTenantIdAndInvestigationId(tenantId, investigationId)
                .map(InvestigationResponse::from)
                .orElseThrow(InvestigationNotFoundException::new);
    }
}
