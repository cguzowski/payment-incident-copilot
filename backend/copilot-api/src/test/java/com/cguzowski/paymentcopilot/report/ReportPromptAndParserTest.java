package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeChunk;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ReportPromptAndParserTest {

    private static final UUID EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID CHUNK_ID = UUID.fromString("97ec5709-147d-458a-a5b4-c95de1a7a32a");
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ReportPromptFactory prompts = new ReportPromptFactory(jsonMapper);
    private final ReportOutputParser parser = new ReportOutputParser(jsonMapper, prompts);

    @Test
    void buildsVersionedBoundedReportInputFromExactSnapshots() {
        ReportPrompt prompt = prompts.build(context());

        assertThat(prompt.promptVersion()).isEqualTo("report-prompt/v1");
        assertThat(prompt.schemaVersion()).isEqualTo("report-v1");
        assertThat(prompt.promptHash()).matches("[0-9a-f]{64}");
        assertThat(prompt.schemaHash()).matches("[0-9a-f]{64}");
        assertThat(prompt.text())
                .contains("Return exactly one JSON object")
                .contains("\"sourceEventId\":\"evt-1\"")
                .contains("\"chunkId\":\"" + CHUNK_ID + "\"")
                .doesNotContain("{{SCHEMA}}", "{{INPUT}}");
    }

    @Test
    void strictlyParsesAndValidatesOneReportV1Object() throws Exception {
        String json = jsonMapper.writeValueAsString(validDocument());

        assertThat(parser.parse(json, context())).isEqualTo(validDocument());
        assertThatThrownBy(() -> parser.parse("preamble\n" + json, context()))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThatThrownBy(() -> parser.parse(json + "\ntrailing", context()))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThatThrownBy(() -> parser.parse(json.replace("MEDIUM", "CERTAIN"), context()))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThatThrownBy(() -> parser.parse(
                        json.replace("\"observations\":[", "\"unsupported\":true,\"observations\":["), context()))
                .isInstanceOf(InvalidReportDocumentException.class);
    }

    private static ReportGenerationContext context() {
        UUID tenantId = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
        UUID investigationId = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
        return new ReportGenerationContext(
                new ReportInvestigationSnapshot(
                        tenantId,
                        investigationId,
                        UUID.fromString("ce22cb8d-10d6-4d6d-9a56-f644ae84573d"),
                        UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07"),
                        "INVESTIGATING",
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Authorization declines elevated",
                        "Synthetic incident."),
                new ReportEvidenceSnapshot(
                        EVIDENCE_ID,
                        "AVAILABLE",
                        EVIDENCE_ID,
                        "authorization-gateway",
                        List.of(new ReportEvidenceObservation(
                                "evt-1", Instant.parse("2026-08-29T08:00:00Z"), "GATEWAY_TIMEOUT", 12))),
                new ReportKnowledgeSnapshot(
                        UUID.fromString("a74f88ed-e295-4caf-9404-a22f733d86ec"),
                        "AVAILABLE",
                        List.of(new ReportKnowledgeChunk(
                                CHUNK_ID,
                                UUID.fromString("a9114c6f-a967-4bd7-a871-7e24716588e4"),
                                "RUNBOOK",
                                "Authorization Decline Runbook",
                                "1.0",
                                "Gateway Failures > Diagnosis",
                                "Inspect upstream gateway timeout telemetry."))));
    }

    private static ReportDocument validDocument() {
        ReportClaim evidence = new ReportClaim("Timeouts were observed.", List.of(EVIDENCE_ID), List.of());
        ReportClaim knowledge = new ReportClaim("Use gateway diagnostics.", List.of(EVIDENCE_ID), List.of(CHUNK_ID));
        return new ReportDocument(
                ReportDisposition.PROPOSED,
                evidence,
                List.of(evidence),
                List.of(knowledge),
                knowledge,
                new ReportConfidence(ReportConfidenceLevel.MEDIUM, "One source is available.", List.of(EVIDENCE_ID)),
                knowledge,
                List.of(),
                List.of(new ReportGap("Deployment history is unavailable.")));
    }
}
