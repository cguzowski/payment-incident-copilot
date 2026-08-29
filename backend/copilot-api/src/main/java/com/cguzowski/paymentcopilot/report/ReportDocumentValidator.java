package com.cguzowski.paymentcopilot.report;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ReportDocumentValidator {

    private static final int MAX_TEXT_LENGTH = 2_000;

    public void validate(ReportDocument report, ReportValidationContext sources) {
        if (report == null) {
            throw invalid("Report document is required");
        }
        if (sources == null) {
            throw invalid("Report validation sources are required");
        }
        if (report.disposition() == null) {
            throw invalid("Report disposition is required");
        }

        validateClaim("Summary", report.summary(), sources, true, false);
        validateClaims("Observation", report.observations(), sources, false);
        validateClaims("Inference", report.inferences(), sources, true);
        validateOptionalClaim("Probable cause", report.probableCause(), sources, true);
        validateConfidence(report.confidence(), sources);
        validateOptionalClaim("Recommendation", report.recommendation(), sources, true);
        validateClaims("Contradiction", report.contradictions(), sources, true);
        validateGaps(report.evidenceGaps());

        if (report.disposition() == ReportDisposition.PROPOSED) {
            if (report.probableCause() == null) {
                throw invalid("A proposed report must include a probable cause");
            }
            if (report.recommendation() == null) {
                throw invalid("A proposed report must include a recommendation");
            }
            if (report.recommendation().knowledgeChunkIds().isEmpty()) {
                throw invalid("A proposed recommendation must cite approved knowledge");
            }
        } else {
            if (report.probableCause() != null) {
                throw invalid("An insufficient-evidence report must not include a probable cause");
            }
            if (report.recommendation() != null) {
                throw invalid("An insufficient-evidence report must not include a recommendation");
            }
            if (report.confidence().level() != ReportConfidenceLevel.LOW) {
                throw invalid("An insufficient-evidence report must have low confidence");
            }
        }
    }

    private void validateClaims(
            String label, List<ReportClaim> claims, ReportValidationContext sources, boolean allowKnowledge) {
        if (claims == null) {
            throw invalid(label + " list is required");
        }
        for (ReportClaim claim : claims) {
            validateClaim(label, claim, sources, true, allowKnowledge);
        }
    }

    private void validateOptionalClaim(
            String label, ReportClaim claim, ReportValidationContext sources, boolean allowKnowledge) {
        if (claim != null) {
            validateClaim(label, claim, sources, true, allowKnowledge);
        }
    }

    private void validateClaim(
            String label,
            ReportClaim claim,
            ReportValidationContext sources,
            boolean requireEvidence,
            boolean allowKnowledge) {
        if (claim == null) {
            throw invalid(label + " is required");
        }
        validateText(label, claim.statement());
        if (requireEvidence && claim.evidenceIds().isEmpty()) {
            throw invalid(label + " must cite evidence");
        }
        validateUnique(label + " evidence", claim.evidenceIds());
        validateUnique(label + " knowledge", claim.knowledgeChunkIds());
        for (UUID evidenceId : claim.evidenceIds()) {
            if (!sources.evidenceIds().contains(evidenceId)) {
                throw invalid(label + " cites unknown evidence " + evidenceId);
            }
        }
        if (!allowKnowledge && !claim.knowledgeChunkIds().isEmpty()) {
            throw invalid("Observations cannot cite approved knowledge");
        }
        for (UUID knowledgeChunkId : claim.knowledgeChunkIds()) {
            if (!sources.knowledgeChunkIds().contains(knowledgeChunkId)) {
                throw invalid(label + " cites unknown approved knowledge " + knowledgeChunkId);
            }
        }
    }

    private void validateConfidence(ReportConfidence confidence, ReportValidationContext sources) {
        if (confidence == null || confidence.level() == null) {
            throw invalid("Confidence and confidence level are required");
        }
        validateText("Confidence rationale", confidence.rationale());
        if (confidence.evidenceIds().isEmpty()) {
            throw invalid("Confidence rationale must cite evidence");
        }
        validateUnique("Confidence evidence", confidence.evidenceIds());
        for (UUID evidenceId : confidence.evidenceIds()) {
            if (!sources.evidenceIds().contains(evidenceId)) {
                throw invalid("Confidence cites unknown evidence " + evidenceId);
            }
        }
    }

    private void validateGaps(List<ReportGap> gaps) {
        if (gaps == null) {
            throw invalid("Evidence gap list is required");
        }
        for (ReportGap gap : gaps) {
            if (gap == null) {
                throw invalid("Evidence gap is required");
            }
            validateText("Evidence gap", gap.description());
        }
    }

    private void validateText(String label, String text) {
        if (text == null || text.isBlank()) {
            throw invalid(label + " must not be blank");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw invalid(label + " exceeds " + MAX_TEXT_LENGTH + " characters");
        }
    }

    private void validateUnique(String label, List<UUID> references) {
        if (references.stream().anyMatch(reference -> reference == null)) {
            throw invalid(label + " contains a null reference");
        }
        Set<UUID> distinct = new HashSet<>(references);
        if (distinct.size() != references.size()) {
            throw invalid(label + " contains duplicate references");
        }
    }

    private InvalidReportDocumentException invalid(String message) {
        return new InvalidReportDocumentException(message);
    }
}
