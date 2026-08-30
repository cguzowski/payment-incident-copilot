package com.cguzowski.syntheticincidentgenerator.generation;

import com.cguzowski.syntheticincidentgenerator.scenario.AlertReferenceCodec;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioDefinition;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioSelector;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

@Service
public class IncidentGenerationService {

    private final ScenarioSelector selector;
    private final AlertReferenceCodec referenceCodec;
    private final AlertIntakeClient alertIntake;
    private final Clock clock;

    public IncidentGenerationService(
            ScenarioSelector selector, AlertReferenceCodec referenceCodec, AlertIntakeClient alertIntake, Clock clock) {
        this.selector = selector;
        this.referenceCodec = referenceCodec;
        this.alertIntake = alertIntake;
        this.clock = clock;
    }

    public GeneratedIncident generate() {
        ScenarioDefinition scenario = selector.select();
        Instant detectedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        String externalAlertId = referenceCodec.encode(scenario.code(), detectedAt);
        AlertIntakeRequest request = new AlertIntakeRequest(
                externalAlertId, scenario.severity(), detectedAt, scenario.title(), scenario.description());
        AlertIntakeResponse accepted = alertIntake.submit(request);
        GeneratedAlert alert = new GeneratedAlert(
                request.externalAlertId(),
                request.severity(),
                request.detectedAt(),
                request.title(),
                request.description());
        return new GeneratedIncident(
                accepted.incidentId(),
                accepted.incidentType(),
                accepted.status(),
                accepted.receivedAt(),
                scenario.code(),
                scenario.rarity(),
                alert,
                scenario.truth());
    }
}
