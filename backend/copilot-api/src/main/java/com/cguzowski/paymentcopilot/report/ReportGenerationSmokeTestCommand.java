package com.cguzowski.paymentcopilot.report;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.report.smoke-test", name = "enabled", havingValue = "true")
class ReportGenerationSmokeTestCommand implements ApplicationRunner {

    static final UUID EVIDENCE_ID = UUID.fromString("61bc908f-e0f6-44e5-b5f6-89baafc662ab");

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportGenerationSmokeTestCommand.class);
    private static final UUID TENANT_ID = UUID.fromString("ecb801cf-7d27-49e0-81ee-3ac21a63114c");
    private static final UUID INVESTIGATION_ID = UUID.fromString("558539d4-f0b5-4ba2-af36-d53a53f251cb");
    private static final UUID RETRIEVAL_ID = UUID.fromString("670a81cb-2138-4b72-aa43-7dd9111ee648");

    private final ReportModel model;
    private final ReportPromptFactory prompts;
    private final ReportOutputParser parser;

    ReportGenerationSmokeTestCommand(ReportModel model, ReportPromptFactory prompts, ReportOutputParser parser) {
        this.model = model;
        this.prompts = prompts;
        this.parser = parser;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        ReportGenerationContext context = syntheticContext();
        try {
            ReportModelResponse response = model.generate(prompts.build(context).text());
            ReportDocument report = parser.parse(response.output(), context);
            LOGGER.info(
                    "Bedrock report smoke test passed: modelId={}, schemaVersion={}, disposition={}, validated=true",
                    model.modelId(),
                    ReportPromptFactory.SCHEMA_VERSION,
                    report.disposition());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Bedrock report smoke test failed safely: modelId={}, schemaVersion={}, validated=false",
                    model.modelId(),
                    ReportPromptFactory.SCHEMA_VERSION);
            throw new IllegalStateException("Bedrock report smoke test failed safely.");
        }
    }

    private static ReportGenerationContext syntheticContext() {
        return new ReportGenerationContext(
                new ReportInvestigationSnapshot(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        UUID.fromString("9b25df2b-f7e4-4d22-a740-9f0f4c09b8ca"),
                        UUID.fromString("71f859f6-f52a-4986-989e-021f08ac597b"),
                        "INVESTIGATING",
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Synthetic authorization declines elevated",
                        "Synthetic report-generation contract smoke test."),
                new ReportEvidenceSnapshot(
                        EVIDENCE_ID,
                        "AVAILABLE",
                        EVIDENCE_ID,
                        "synthetic-authorization-gateway",
                        List.of(new ReportEvidenceObservation(
                                "synthetic-event-1", Instant.parse("2026-08-29T08:00:00Z"), "GATEWAY_TIMEOUT", 1))),
                new ReportKnowledgeSnapshot(RETRIEVAL_ID, "NO_MATCH", List.of()));
    }
}
