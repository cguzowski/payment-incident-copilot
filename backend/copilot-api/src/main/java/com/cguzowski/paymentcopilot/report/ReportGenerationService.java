package com.cguzowski.paymentcopilot.report;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class ReportGenerationService {

    private static final String UNAVAILABLE_DETAIL = "The report model is unavailable.";
    private static final String TIMED_OUT_DETAIL = "The report model timed out.";
    private static final String MALFORMED_DETAIL = "The model response did not match report-v1.";

    private final ReportContextAssembler contexts;
    private final ReportGenerationPersistenceService persistence;
    private final ReportModel model;
    private final ReportPromptFactory prompts;
    private final ReportOutputParser parser;
    private final ReportIdentifierGenerator identifiers;
    private final Clock clock;

    ReportGenerationService(
            ReportContextAssembler contexts,
            ReportGenerationPersistenceService persistence,
            ReportModel model,
            ReportPromptFactory prompts,
            ReportOutputParser parser,
            ReportIdentifierGenerator identifiers,
            Clock clock) {
        this.contexts = contexts;
        this.persistence = persistence;
        this.model = model;
        this.prompts = prompts;
        this.parser = parser;
        this.identifiers = identifiers;
        this.clock = clock;
    }

    ReportGenerationResponse generate(UUID tenantId, UUID investigationId, UUID operatorId) {
        ReportGenerationContext context =
                contexts.find(tenantId, investigationId).orElseThrow(ReportInvestigationNotFoundException::new);
        if (!"INVESTIGATING".equals(context.investigation().incidentStatus())) {
            throw new ReportGenerationConflictException("The investigation is not ready for report generation.");
        }

        ReportPrompt prompt = prompts.build(context);
        ReportGenerationAttempt started = ReportGenerationAttempt.started(
                identifiers.next(), operatorId, Instant.now(clock), context, model.modelId(), prompt);
        if (!persistence.start(started)) {
            throw new ReportGenerationConflictException("Report generation is already active or complete.");
        }

        ReportModelResponse modelResponse = null;
        try {
            modelResponse = model.generate(prompt.text());
            ReportDocument document = parser.parse(modelResponse.output(), context);
            ReportGenerationAttempt completed = started.completeAvailable(Instant.now(clock), modelResponse, document);
            if (!persistence.completeAvailable(completed)) {
                throw new IllegalStateException("The available report attempt could not be completed.");
            }
            return ReportGenerationResponse.from(completed);
        } catch (ReportModelTimedOutException exception) {
            return completeFailure(started, ReportGenerationStatus.TIMED_OUT, null, TIMED_OUT_DETAIL);
        } catch (ReportModelUnavailableException exception) {
            return completeFailure(started, ReportGenerationStatus.UNAVAILABLE, null, UNAVAILABLE_DETAIL);
        } catch (InvalidReportDocumentException exception) {
            return completeFailure(
                    started,
                    ReportGenerationStatus.MALFORMED,
                    modelResponse == null ? null : modelResponse.providerRequestId(),
                    MALFORMED_DETAIL);
        }
    }

    List<ReportGenerationResponse> history(UUID tenantId, UUID investigationId) {
        if (contexts.findInvestigation(tenantId, investigationId).isEmpty()) {
            throw new ReportInvestigationNotFoundException();
        }
        return persistence.findAll(tenantId, investigationId).stream()
                .map(ReportGenerationResponse::from)
                .toList();
    }

    private ReportGenerationResponse completeFailure(
            ReportGenerationAttempt started, ReportGenerationStatus status, String providerRequestId, String detail) {
        ReportGenerationAttempt completed =
                started.completeFailure(status, Instant.now(clock), providerRequestId, detail);
        if (!persistence.completeFailure(completed)) {
            throw new IllegalStateException("The failed report attempt could not be completed.");
        }
        return ReportGenerationResponse.from(completed);
    }
}
