package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import com.cguzowski.paymentcopilot.evidence.ReportEvidenceSnapshot;
import com.cguzowski.paymentcopilot.incident.ReportInvestigationSnapshot;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeChunk;
import com.cguzowski.paymentcopilot.knowledge.retrieval.ReportKnowledgeSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ReportPromptAndParserTest {

    private static final UUID EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID SECOND_EVIDENCE_ID = UUID.fromString("a14aa5e9-35c9-4e4a-89e8-35b628c59dda");
    private static final UUID CHUNK_ID = UUID.fromString("97ec5709-147d-458a-a5b4-c95de1a7a32a");
    private static final UUID SECOND_CHUNK_ID = UUID.fromString("6f0581d2-e8f0-4ce5-a5a2-282db58ea76a");
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ReportPromptFactory prompts = new ReportPromptFactory(jsonMapper);
    private final ReportOutputParser parser = new ReportOutputParser(jsonMapper, prompts);

    @Test
    void buildsVersionedBoundedReportInputFromExactSnapshots() {
        ReportPrompt prompt = prompts.build(context());

        assertThat(prompt.promptVersion()).isEqualTo("report-prompt/v4");
        assertThat(prompt.schemaVersion()).isEqualTo("report-v1");
        assertThat(prompt.promptHash()).matches("[0-9a-f]{64}");
        assertThat(prompt.schemaHash()).matches("[0-9a-f]{64}");
        assertThat(prompt.outputSchema())
                .contains("\"evidenceId\"")
                .contains("\"enum\":[\"" + EVIDENCE_ID + "\"]")
                .contains("\"knowledgeChunkId\"")
                .contains("\"enum\":[\"" + CHUNK_ID + "\"]");
        assertThat(prompt.text())
                .contains("Return exactly one JSON object")
                .contains("Use PROPOSED when")
                .contains("Use only latestAttemptId or applicableAttemptId in evidenceIds")
                .contains("never repeat an identifier")
                .contains("probableCause and recommendation must both be null")
                .contains("at most 2 observations")
                .contains("Keep every statement and rationale under 300 characters")
                .contains("\"sourceEventId\":\"evt-1\"")
                .contains("\"chunkId\":\"" + CHUNK_ID + "\"")
                .doesNotContain("\"$defs\"")
                .doesNotContain("{{SCHEMA}}", "{{INPUT}}");
    }

    @Test
    void narrowsCitationArrayBoundsToDistinctEligibleSources() throws Exception {
        JsonNode oneSource = jsonMapper.readTree(prompts.build(context()).outputSchema());

        assertThat(oneSource.at("/$defs/claim/properties/evidenceIds/maxItems").intValue())
                .isEqualTo(1);
        assertThat(oneSource
                        .at("/$defs/evidenceOnlyClaim/properties/evidenceIds/maxItems")
                        .intValue())
                .isEqualTo(1);
        assertThat(oneSource
                        .at("/$defs/knowledgeClaim/properties/evidenceIds/maxItems")
                        .intValue())
                .isEqualTo(1);
        assertThat(oneSource
                        .at("/$defs/confidence/properties/evidenceIds/maxItems")
                        .intValue())
                .isEqualTo(1);
        assertThat(oneSource
                        .at("/$defs/claim/properties/knowledgeChunkIds/maxItems")
                        .intValue())
                .isEqualTo(1);
        assertThat(oneSource
                        .at("/$defs/knowledgeClaim/properties/knowledgeChunkIds/maxItems")
                        .intValue())
                .isEqualTo(1);
        assertThat(oneSource
                        .at("/$defs/evidenceOnlyClaim/properties/knowledgeChunkIds/maxItems")
                        .intValue())
                .isZero();

        JsonNode twoSources =
                jsonMapper.readTree(prompts.build(contextWithTwoSources()).outputSchema());
        assertThat(twoSources.at("/$defs/claim/properties/evidenceIds/maxItems").intValue())
                .isEqualTo(2);
        assertThat(twoSources
                        .at("/$defs/confidence/properties/evidenceIds/maxItems")
                        .intValue())
                .isEqualTo(2);
        assertThat(twoSources
                        .at("/$defs/claim/properties/knowledgeChunkIds/maxItems")
                        .intValue())
                .isEqualTo(2);
        assertThat(twoSources
                        .at("/$defs/knowledgeClaim/properties/knowledgeChunkIds/maxItems")
                        .intValue())
                .isEqualTo(2);

        JsonNode manySources =
                jsonMapper.readTree(prompts.build(contextWithKnowledgeCount(11)).outputSchema());
        assertThat(manySources
                        .at("/$defs/claim/properties/knowledgeChunkIds/maxItems")
                        .intValue())
                .isEqualTo(10);
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

    @Test
    void schemaEncodesTheConciseEvidenceOnlyObservationContract() throws Exception {
        ReportDocument valid = validDocument();
        String summaryWithKnowledge = jsonMapper.writeValueAsString(new ReportDocument(
                valid.disposition(),
                new ReportClaim("Timeouts were observed.", List.of(EVIDENCE_ID), List.of(CHUNK_ID)),
                valid.observations(),
                valid.inferences(),
                valid.probableCause(),
                valid.confidence(),
                valid.recommendation(),
                valid.contradictions(),
                valid.evidenceGaps()));
        String observationWithKnowledge = jsonMapper.writeValueAsString(new ReportDocument(
                valid.disposition(),
                valid.summary(),
                List.of(new ReportClaim("Timeouts were observed.", List.of(EVIDENCE_ID), List.of(CHUNK_ID))),
                valid.inferences(),
                valid.probableCause(),
                valid.confidence(),
                valid.recommendation(),
                valid.contradictions(),
                valid.evidenceGaps()));
        String excessiveObservations = jsonMapper.writeValueAsString(new ReportDocument(
                valid.disposition(),
                valid.summary(),
                List.of(valid.summary(), valid.summary(), valid.summary()),
                valid.inferences(),
                valid.probableCause(),
                valid.confidence(),
                valid.recommendation(),
                valid.contradictions(),
                valid.evidenceGaps()));

        assertThatThrownBy(() -> parser.parse(summaryWithKnowledge, context()))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThatThrownBy(() -> parser.parse(observationWithKnowledge, context()))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThatThrownBy(() -> parser.parse(excessiveObservations, context()))
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

    private static ReportGenerationContext contextWithTwoSources() {
        ReportGenerationContext base = context();
        return new ReportGenerationContext(
                base.investigation(),
                new ReportEvidenceSnapshot(
                        EVIDENCE_ID,
                        "PARTIAL",
                        SECOND_EVIDENCE_ID,
                        "authorization-gateway",
                        base.evidence().observations()),
                new ReportKnowledgeSnapshot(
                        base.knowledge().retrievalId(),
                        "AVAILABLE",
                        List.of(
                                base.knowledge().chunks().getFirst(),
                                new ReportKnowledgeChunk(
                                        SECOND_CHUNK_ID,
                                        UUID.fromString("0f38f904-e31a-4600-b6ef-4a0556c084eb"),
                                        "POLICY",
                                        "Evidence Policy",
                                        "1.0",
                                        "Citation rules",
                                        "Cite approved evidence."))));
    }

    private static ReportGenerationContext contextWithKnowledgeCount(int count) {
        ReportGenerationContext base = context();
        List<ReportKnowledgeChunk> chunks = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            chunks.add(new ReportKnowledgeChunk(
                    UUID.nameUUIDFromBytes(("chunk-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    UUID.nameUUIDFromBytes(("document-" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    "POLICY",
                    "Evidence Policy " + index,
                    "1.0",
                    "Citation rules",
                    "Cite approved evidence."));
        }
        return new ReportGenerationContext(
                base.investigation(),
                base.evidence(),
                new ReportKnowledgeSnapshot(base.knowledge().retrievalId(), "AVAILABLE", List.copyOf(chunks)));
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
