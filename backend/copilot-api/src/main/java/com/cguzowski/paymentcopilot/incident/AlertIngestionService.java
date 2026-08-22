package com.cguzowski.paymentcopilot.incident;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AlertIngestionService {

    private final IncidentRepository incidentRepository;
    private final Clock clock;

    AlertIngestionService(IncidentRepository incidentRepository, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    @Transactional
    AlertIngestionResult ingest(IngestAlertCommand command) {
        Incident incident = new Incident(
                UUID.randomUUID(),
                command.tenantId(),
                command.externalAlertId(),
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                command.severity(),
                IncidentStatus.NEW,
                command.title(),
                command.description(),
                command.detectedAt(),
                Instant.now(clock));

        if (incidentRepository.insertIfAbsent(incident)) {
            return new AlertIngestionResult(incident, true);
        }

        Incident existing = incidentRepository
                .findByTenantIdAndExternalAlertId(command.tenantId(), command.externalAlertId())
                .orElseThrow(() -> new IllegalStateException("Idempotent incident lookup failed"));
        return new AlertIngestionResult(existing, false);
    }
}
