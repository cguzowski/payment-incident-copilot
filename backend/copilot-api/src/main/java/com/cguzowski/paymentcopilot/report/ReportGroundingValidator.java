package com.cguzowski.paymentcopilot.report;

import com.cguzowski.paymentcopilot.evidence.ReportEvidenceObservation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** Bounded tuple and explicit source-role checks; not a general prose entailment checker. */
final class ReportGroundingValidator {
    private static final Pattern SOURCE = Pattern.compile("\\bE\\d{2}\\b");
    private static final Pattern SOURCE_CLAUSE =
            Pattern.compile("\\b(E\\d{2})\\b([^.;\\n]*?)(?=\\bE\\d{2}\\b|[.;\\n]|$)");
    private static final List<Pattern> STAGE_ROLES = List.of(
            stageRole("authorization", "approv|response|confirm"),
            stageRole("capture", "acknowledg|confirm"),
            stageRole("refund", "acknowledg|confirm"),
            stageRole("settlement", "receipt|confirm"));

    static String observationStatement(ReportEvidenceObservation observation) {
        return "sourceEventId=" + observation.sourceEventId() + "; observedAt=" + observation.observedAt()
                + "; errorCode=" + observation.errorCode() + "; count=" + observation.count();
    }

    void validate(ReportDocument report, ReportGenerationContext context) {
        Set<String> eligible = new HashSet<>();
        context.evidence().observations().forEach(observation -> eligible.add(observationStatement(observation)));
        UUID owner = context.evidence().applicableAttemptId();
        Set<String> seen = new HashSet<>();
        for (ReportClaim observation : report.observations()) {
            if (!eligible.contains(observation.statement())
                    || !observation.evidenceIds().equals(owner == null ? List.of() : List.of(owner))
                    || !seen.add(observation.statement())) {
                throw invalid();
            }
        }
        validateSources(report.probableCause(), context);
        validateSources(report.recommendation(), context);
        report.inferences().forEach(claim -> validateSources(claim, context));
        report.contradictions().forEach(claim -> validateSources(claim, context));
    }

    private void validateSources(ReportClaim claim, ReportGenerationContext context) {
        if (claim == null) {
            return;
        }
        List<String> passages = context.knowledge().chunks().stream()
                .filter(chunk -> claim.knowledgeChunkIds().contains(chunk.chunkId()))
                .map(chunk -> chunk.rawContent())
                .toList();
        Set<String> suppliedSources = new HashSet<>();
        for (String passage : passages) {
            SOURCE.matcher(passage).results().forEach(match -> suppliedSources.add(match.group()));
        }
        if (SOURCE.matcher(claim.statement()).results().anyMatch(match -> !suppliedSources.contains(match.group()))) {
            throw invalid();
        }
        String statement = claim.statement().replaceAll("\\s+", " ");
        for (Pattern role : STAGE_ROLES) {
            if (role.matcher(statement).find()) {
                Set<String> supplied = new HashSet<>();
                passages.forEach(passage -> supplied.addAll(roleSources(passage, role)));
                Set<String> requested = roleSources(statement, role);
                if (requested.isEmpty() || !supplied.containsAll(requested)) {
                    throw invalid();
                }
            }
        }
    }

    private static Pattern stageRole(String stage, String nouns) {
        return Pattern.compile("\\b" + stage + "\\b.{0,60}\\b(?:" + nouns + ")\\w*\\b", Pattern.CASE_INSENSITIVE);
    }

    private static Set<String> roleSources(String text, Pattern role) {
        Set<String> result = new HashSet<>();
        SOURCE_CLAUSE.matcher(text.replaceAll("\\s+", " ")).results().forEach(match -> {
            if (role.matcher(match.group(2)).find()) {
                result.add(match.group(1));
            }
        });
        return result;
    }

    private static InvalidReportDocumentException invalid() {
        return new InvalidReportDocumentException("The model response did not match report-v1 grounding constraints.");
    }
}
