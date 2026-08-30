package com.cguzowski.paymentcopilot.report;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportDocumentValidatorTest {

    private static final UUID EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");
    private static final UUID KNOWLEDGE_CHUNK_ID = UUID.fromString("97ec5709-147d-458a-a5b4-c95de1a7a32a");
    private static final ReportValidationContext SOURCES =
            new ReportValidationContext(Set.of(EVIDENCE_ID), Set.of(KNOWLEDGE_CHUNK_ID));

    private final ReportDocumentValidator validator = new ReportDocumentValidator();

    @Test
    void acceptsCitedProposedReportMatchingReportV1() {
        ReportDocument report = proposedReport();

        assertThatCode(() -> validator.validate(report, SOURCES)).doesNotThrowAnyException();
    }

    @Test
    void rejectsObservationThatReferencesKnowledgeAsObservedFact() {
        ReportDocument valid = proposedReport();
        ReportClaim invalidObservation =
                new ReportClaim("Timeouts were observed.", List.of(EVIDENCE_ID), List.of(KNOWLEDGE_CHUNK_ID));
        ReportDocument invalid = new ReportDocument(
                valid.disposition(),
                valid.summary(),
                List.of(invalidObservation),
                valid.inferences(),
                valid.probableCause(),
                valid.confidence(),
                valid.recommendation(),
                valid.contradictions(),
                valid.evidenceGaps());

        assertThatThrownBy(() -> validator.validate(invalid, SOURCES))
                .isInstanceOf(InvalidReportDocumentException.class)
                .hasMessageContaining("Observations cannot cite approved knowledge");
    }

    @Test
    void rejectsUnknownSourceReference() {
        ReportDocument valid = proposedReport();
        ReportClaim invalidSummary = new ReportClaim(
                "The incident remains under investigation.",
                List.of(UUID.fromString("0656aedc-36e8-42a4-9423-f6a80a56a931")),
                List.of());
        ReportDocument invalid = new ReportDocument(
                valid.disposition(),
                invalidSummary,
                valid.observations(),
                valid.inferences(),
                valid.probableCause(),
                valid.confidence(),
                valid.recommendation(),
                valid.contradictions(),
                valid.evidenceGaps());

        assertThatThrownBy(() -> validator.validate(invalid, SOURCES))
                .isInstanceOf(InvalidReportDocumentException.class)
                .hasMessageContaining("unknown evidence");
    }

    @Test
    void acceptsExplicitInsufficientEvidenceWithoutCauseOrRecommendation() {
        ReportDocument report = new ReportDocument(
                ReportDisposition.INSUFFICIENT_EVIDENCE,
                claim("Available evidence is insufficient for a probable cause."),
                List.of(claim("The latest collection attempt was unavailable.")),
                List.of(),
                null,
                new ReportConfidence(
                        ReportConfidenceLevel.LOW, "Required evidence is unavailable.", List.of(EVIDENCE_ID)),
                null,
                List.of(),
                List.of(new ReportGap("A current service-error observation is unavailable.")));

        assertThatCode(() -> validator.validate(report, SOURCES)).doesNotThrowAnyException();
    }

    @Test
    void rejectsInsufficientEvidenceWithProbableCause() {
        ReportDocument report = new ReportDocument(
                ReportDisposition.INSUFFICIENT_EVIDENCE,
                claim("Available evidence is insufficient for a reliable conclusion."),
                List.of(claim("The latest collection attempt was unavailable.")),
                List.of(),
                claimWithKnowledge("Gateway instability caused the decline spike."),
                new ReportConfidence(
                        ReportConfidenceLevel.LOW, "Required evidence is unavailable.", List.of(EVIDENCE_ID)),
                null,
                List.of(),
                List.of(new ReportGap("A current service-error observation is unavailable.")));

        assertThatThrownBy(() -> validator.validate(report, SOURCES))
                .isInstanceOf(InvalidReportDocumentException.class)
                .hasMessageContaining("must not include a probable cause");
    }

    private static ReportDocument proposedReport() {
        return new ReportDocument(
                ReportDisposition.PROPOSED,
                claim("Authorization declines correlate with gateway timeouts."),
                List.of(claim("Twelve gateway timeout errors were observed.")),
                List.of(claimWithKnowledge("The error pattern is consistent with upstream instability.")),
                claimWithKnowledge("Upstream gateway instability is the probable cause."),
                new ReportConfidence(
                        ReportConfidenceLevel.MEDIUM,
                        "The observed error pattern supports the cause, but only one evidence source is available.",
                        List.of(EVIDENCE_ID)),
                claimWithKnowledge("Follow the approved gateway-failure diagnostic steps."),
                List.of(),
                List.of(new ReportGap("Deployment history has not been collected.")));
    }

    private static ReportClaim claim(String statement) {
        return new ReportClaim(statement, List.of(EVIDENCE_ID), List.of());
    }

    private static ReportClaim claimWithKnowledge(String statement) {
        return new ReportClaim(statement, List.of(EVIDENCE_ID), List.of(KNOWLEDGE_CHUNK_ID));
    }
}
