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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class ReportGroundingValidationTest {
    private static final UUID EVIDENCE = UUID.randomUUID();
    private static final UUID LATEST = UUID.randomUUID();
    private static final UUID CHUNK = UUID.randomUUID();
    private static final UUID OTHER_CHUNK = UUID.randomUUID();
    private static final Instant TIME = Instant.parse("2026-10-04T12:00:00Z");
    private static final String TUPLE =
            "sourceEventId=evt-1; observedAt=2026-10-04T12:00:00Z; errorCode=LATE_RESPONSE; count=8";
    private static final String GUIDANCE =
            "Request E03 authorization approval. Obtain E04 capture requests and final acknowledgements with operation IDs. No blind retry.";
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ReportPromptFactory prompts = new ReportPromptFactory(mapper);
    private final ReportOutputParser parser = new ReportOutputParser(mapper, prompts);

    @Test
    void preservesExactObservedCodeAndCount() throws Exception {
        var document = report(List.of(claim(TUPLE)), "Request E04 capture acknowledgements.", List.of(CHUNK));
        assertThat(parser.parse(mapper.writeValueAsString(document), context("AVAILABLE", observations(), GUIDANCE)))
                .isEqualTo(document);
        rejects(TUPLE.replace("count=8", "count=1"));
    }

    @Test
    void detectsCountAssignedToWrongErrorCode() throws Exception {
        var events =
                List.of(observations().getFirst(), new ReportEvidenceObservation("evt-2", TIME, "CONNECTION_RESET", 3));
        var swapped = TUPLE.replace("evt-1", "evt-2").replace("LATE_RESPONSE", "CONNECTION_RESET");
        var document = report(List.of(claim(swapped)), "Request E04 capture acknowledgements.", List.of(CHUNK));
        assertThatThrownBy(
                        () -> parser.parse(mapper.writeValueAsString(document), context("AVAILABLE", events, GUIDANCE)))
                .isInstanceOf(InvalidReportDocumentException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "One LATE_RESPONSE was observed.",
                "LATE_RESPONSE count=1",
                "LATE_RESPONSE count=8",
                "sourceEventId=evt-1; observedAt=2026-10-04T12:00:00Z; errorCode=LATE_RESPONSE; count=8; count=1",
                "sourceEventId=evt-1; observedAt=2026-10-04T12:00:00Z; errorCode=LATE_RESPONSE; count=8. Guidance confirms capture."
            })
    void rejectsParaphrasesAndAdditionalAssertions(String statement) {
        rejects(statement);
    }

    @Test
    void missingObservationIsNotFabricated() throws Exception {
        var document = insufficient(List.of(claim(TUPLE)));
        assertThatThrownBy(() ->
                        parser.parse(mapper.writeValueAsString(document), context("UNAVAILABLE", List.of(), GUIDANCE)))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThat(parser.parse(
                        mapper.writeValueAsString(insufficient(List.of())),
                        context("UNAVAILABLE", List.of(), GUIDANCE)))
                .isEqualTo(insufficient(List.of()));
        rejects(TUPLE.replace("evt-1", "invented"));
        rejects(TUPLE.replace("12:00:00", "12:01:00"));
    }

    @Test
    void repeatedSignalsPreserveTheirBoundedSourceContext() throws Exception {
        var repeated =
                List.of(observations().getFirst(), new ReportEvidenceObservation("evt-2", TIME, "LATE_RESPONSE", 3));
        var second = TUPLE.replace("evt-1", "evt-2").replace("count=8", "count=3");
        var document =
                report(List.of(claim(TUPLE), claim(second)), "Request E04 capture acknowledgements.", List.of(CHUNK));
        assertThat(parser.parse(mapper.writeValueAsString(document), context("AVAILABLE", repeated, GUIDANCE)))
                .isEqualTo(document);
        assertThatThrownBy(() -> parser.parse(
                        mapper.writeValueAsString(report(
                                List.of(claim(TUPLE), claim(TUPLE)),
                                "Request E04 capture acknowledgements.",
                                List.of(CHUNK))),
                        context("AVAILABLE", repeated, GUIDANCE)))
                .isInstanceOf(InvalidReportDocumentException.class);
        rejects(TUPLE.replace("count=8", "count=11"));
    }

    @Test
    void historicalTuplesMustCiteApplicableAttemptOnly() throws Exception {
        var context = context("UNAVAILABLE", observations(), GUIDANCE);
        assertThat(parser.parse(mapper.writeValueAsString(insufficient(List.of(claim(TUPLE)))), context)
                        .observations())
                .containsExactly(claim(TUPLE));
        var wrong = new ReportClaim(TUPLE, List.of(LATEST), List.of());
        assertThatThrownBy(() -> parser.parse(mapper.writeValueAsString(insufficient(List.of(wrong))), context))
                .isInstanceOf(InvalidReportDocumentException.class);
    }

    @Test
    void captureConfirmationRequestUsesSupportedSourceRole() throws Exception {
        for (String invalid : List.of(
                "Request E03 capture acknowledgements.",
                "Request capture acknowledgements.",
                "Request E04 unrelated records and E03 capture confirmations.")) {
            assertThatThrownBy(() -> parser.parse(
                            mapper.writeValueAsString(report(List.of(claim(TUPLE)), invalid, List.of(CHUNK))),
                            context("AVAILABLE", observations(), GUIDANCE)))
                    .isInstanceOf(InvalidReportDocumentException.class);
        }
        var supported = report(List.of(claim(TUPLE)), "Request E04 capture acknowledgements.", List.of(CHUNK));
        assertThat(parser.parse(mapper.writeValueAsString(supported), context("AVAILABLE", observations(), GUIDANCE)))
                .isEqualTo(supported);
    }

    @Test
    void uncitedOrAbsentGuidanceCannotSupplySourceRole() throws Exception {
        var document = report(List.of(claim(TUPLE)), "Request E04 capture acknowledgements.", List.of(OTHER_CHUNK));
        assertThatThrownBy(() -> parser.parse(
                        mapper.writeValueAsString(document), context("AVAILABLE", observations(), GUIDANCE)))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThatThrownBy(() -> parser.parse(
                        mapper.writeValueAsString(
                                report(List.of(claim(TUPLE)), "Request E09 reconciliation records.", List.of(CHUNK))),
                        context("AVAILABLE", observations(), GUIDANCE)))
                .isInstanceOf(InvalidReportDocumentException.class);
        var noMapping = report(List.of(claim(TUPLE)), "Request owner review of missing records.", List.of(OTHER_CHUNK));
        assertThat(parser.parse(mapper.writeValueAsString(noMapping), context("AVAILABLE", observations(), GUIDANCE)))
                .isEqualTo(noMapping);
    }

    @Test
    void sourceMappingComesFromCitedTextRatherThanHardCodedIdentifier() throws Exception {
        var document = report(List.of(claim(TUPLE)), "Request E07 capture acknowledgements.", List.of(CHUNK));
        assertThat(parser.parse(
                        mapper.writeValueAsString(document),
                        context("AVAILABLE", observations(), GUIDANCE.replace("E04", "E07"))))
                .isEqualTo(document);
    }

    @Test
    void sourceRoleMappingSurvivesPdfWhitespaceAndDoesNotCrossOtherSourceCodes() throws Exception {
        var document = report(List.of(claim(TUPLE)), "Request E04 capture acknowledgements.", List.of(CHUNK));
        assertThat(parser.parse(
                        mapper.writeValueAsString(document),
                        context(
                                "AVAILABLE",
                                observations(),
                                "Obtain E04 capture requests and final\nacknowledgements with operation IDs.")))
                .isEqualTo(document);
        assertThatThrownBy(() -> parser.parse(
                        mapper.writeValueAsString(document),
                        context(
                                "AVAILABLE",
                                observations(),
                                "Obtain E04 authorization records and E03 capture acknowledgements.")))
                .isInstanceOf(InvalidReportDocumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"authorization responses", "refund confirmations", "settlement receipts"})
    void rejectsSourceIdentifierAssignedToWrongStageRole(String role) throws Exception {
        String guidance = "Obtain E03 " + role + ". Obtain E04 capture acknowledgements.";
        var wrong = report(List.of(claim(TUPLE)), "Request E04 " + role + ".", List.of(CHUNK));
        var correct = report(List.of(claim(TUPLE)), "Request E03 " + role + ".", List.of(CHUNK));
        assertThatThrownBy(() ->
                        parser.parse(mapper.writeValueAsString(wrong), context("AVAILABLE", observations(), guidance)))
                .isInstanceOf(InvalidReportDocumentException.class);
        assertThat(parser.parse(mapper.writeValueAsString(correct), context("AVAILABLE", observations(), guidance)))
                .isEqualTo(correct);
    }

    @Test
    void treatsTemplateTokensInSnapshotAndTuplesAsLiteralData() throws Exception {
        var events = List.of(new ReportEvidenceObservation("evt-{{INPUT}}-$1", TIME, "LATE_RESPONSE", 8));
        var context = context("AVAILABLE", events, "Literal {{OBSERVATIONS}} and $1 must survive.");
        var prompt = prompts.build(context);
        String input = prompt.text()
                .substring(prompt.text().indexOf("REPORT_INPUT:\n") + "REPORT_INPUT:\n".length())
                .trim();
        assertThat(input).isEqualTo(mapper.writeValueAsString(context));
        assertThat(prompt.text()).contains("sourceEventId=evt-{{INPUT}}-$1;");
    }

    @Test
    void promptAndSchemaExposeExactTuplesWithoutChangingReportV1() throws Exception {
        var prompt = prompts.build(context("AVAILABLE", observations(), GUIDANCE));
        assertThat(prompt.promptVersion()).isEqualTo("report-prompt/v9");
        assertThat(prompt.schemaVersion()).isEqualTo("report-v1");
        assertThat(prompt.text()).contains(TUPLE, "Do not paraphrase observations", "E##", "capture acknowledgements");
        assertThat(mapper.readTree(prompt.outputSchema())
                        .at("/properties/observations/items/properties/statement/enum")
                        .toString())
                .contains(TUPLE);
    }

    private void rejects(String statement) {
        assertThatThrownBy(() -> parser.parse(
                        mapper.writeValueAsString(report(
                                List.of(claim(statement)), "Request E04 capture acknowledgements.", List.of(CHUNK))),
                        context("AVAILABLE", observations(), GUIDANCE)))
                .isInstanceOf(InvalidReportDocumentException.class);
    }

    private static List<ReportEvidenceObservation> observations() {
        return List.of(new ReportEvidenceObservation("evt-1", TIME, "LATE_RESPONSE", 8));
    }

    private static ReportClaim claim(String text) {
        return new ReportClaim(text, List.of(EVIDENCE), List.of());
    }

    private static ReportDocument report(List<ReportClaim> observations, String recommendation, List<UUID> chunks) {
        return new ReportDocument(
                ReportDisposition.PROPOSED,
                claim("Late responses observed."),
                observations,
                List.of(),
                claim("Response delay is observed; outcome remains unknown."),
                new ReportConfidence(ReportConfidenceLevel.MEDIUM, "Aggregate only.", List.of(EVIDENCE)),
                new ReportClaim(recommendation, List.of(EVIDENCE), chunks),
                List.of(),
                List.of(new ReportGap("Final outcome unknown.")));
    }

    private static ReportDocument insufficient(List<ReportClaim> observations) {
        return new ReportDocument(
                ReportDisposition.INSUFFICIENT_EVIDENCE,
                claim("Evidence unavailable."),
                observations,
                List.of(),
                null,
                new ReportConfidence(ReportConfidenceLevel.LOW, "Latest evidence unavailable.", List.of(LATEST)),
                null,
                List.of(),
                List.of(new ReportGap("Latest evidence unavailable.")));
    }

    private static ReportGenerationContext context(
            String status, List<ReportEvidenceObservation> observations, String guidance) {
        return new ReportGenerationContext(
                new ReportInvestigationSnapshot(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "INVESTIGATING",
                        "CAPTURE_FAILURE_SPIKE",
                        "Synthetic",
                        "Synthetic"),
                new ReportEvidenceSnapshot(
                        status.equals("AVAILABLE") ? EVIDENCE : LATEST,
                        status,
                        EVIDENCE,
                        "capture-service",
                        observations),
                new ReportKnowledgeSnapshot(
                        UUID.randomUUID(),
                        "AVAILABLE",
                        List.of(
                                new ReportKnowledgeChunk(
                                        CHUNK, UUID.randomUUID(), "RUNBOOK", "Capture", "1.0", "Procedure", guidance),
                                new ReportKnowledgeChunk(
                                        OTHER_CHUNK,
                                        UUID.randomUUID(),
                                        "POLICY",
                                        "Review",
                                        "1.0",
                                        "Review",
                                        "Request owner review of missing records."))));
    }
}
