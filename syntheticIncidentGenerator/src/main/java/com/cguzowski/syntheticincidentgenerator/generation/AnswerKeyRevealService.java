package com.cguzowski.syntheticincidentgenerator.generation;

import com.cguzowski.syntheticincidentgenerator.scenario.AlertReferenceCodec;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioOracleCatalog;
import com.cguzowski.syntheticincidentgenerator.scenario.ScenarioOracleEntry;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnswerKeyRevealService {

    private static final Set<String> TERMINAL_STATUSES = Set.of("APPROVED", "REJECTED");
    private final CopilotIncidentReviewClient incidentClient;
    private final ScenarioOracleCatalog oracle;
    private final AlertReferenceCodec referenceCodec;
    private final AnswerKeyRevealAuditSink auditSink;
    private final Clock clock;

    AnswerKeyRevealService(
            CopilotIncidentReviewClient incidentClient,
            ScenarioOracleCatalog oracle,
            AlertReferenceCodec referenceCodec,
            AnswerKeyRevealAuditSink auditSink,
            Clock clock) {
        this.incidentClient = incidentClient;
        this.oracle = oracle;
        this.referenceCodec = referenceCodec;
        this.auditSink = auditSink;
        this.clock = clock;
    }

    public AnswerKeyRevealResponse reveal(UUID incidentId, UUID operatorId) {
        IncidentReviewState incident = incidentClient.find(incidentId);
        if (!TERMINAL_STATUSES.contains(incident.status())) {
            throw new AnswerKeyNotReadyException();
        }
        String scenarioCode = referenceCodec
                .decode(incident.externalAlertId())
                .map(decoded -> decoded.scenarioCode())
                .orElseThrow(AnswerKeyUnavailableException::new);
        ScenarioOracleEntry scenario = oracle.findByCode(scenarioCode).orElseThrow(AnswerKeyUnavailableException::new);
        Instant revealedAt = Instant.now(clock);
        auditSink.record(new AnswerKeyRevealEvent(
                incidentId, operatorId, incident.status(), scenarioCode, oracle.version(), revealedAt));
        return new AnswerKeyRevealResponse(
                incidentId, incident.status(), operatorId, revealedAt, oracle.version(), scenario.truth());
    }
}
