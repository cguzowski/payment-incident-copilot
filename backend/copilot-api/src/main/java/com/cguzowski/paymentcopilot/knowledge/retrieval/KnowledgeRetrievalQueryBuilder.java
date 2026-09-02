package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class KnowledgeRetrievalQueryBuilder {

    static final String TEMPLATE_VERSION = "knowledge-query/v2";
    static final int MAXIMUM_QUERY_LENGTH = 2000;
    private static final int MAXIMUM_TITLE_LENGTH = 400;
    private static final int MAXIMUM_SERVICE_LENGTH = 256;
    private static final int MAXIMUM_ERRORS_LENGTH = 512;

    DerivedKnowledgeQuery build(KnowledgeRetrievalContext context) {
        LinkedHashSet<UUID> evidenceIds = new LinkedHashSet<>();
        KnowledgeEvidenceReference evidence = context.evidence();
        String evidenceStatus = evidence == null
                ? "NOT_COLLECTED"
                : evidence.latestStatus() == null ? "UNKNOWN" : evidence.latestStatus();
        String statusLine = "Observed evidence status: " + evidenceStatus;
        List<String> lines = new ArrayList<>();
        if (evidence == null) {
            addIncidentOnlyQuery(lines, context, statusLine);
        } else {
            if (evidence.latestAttemptId() != null) {
                evidenceIds.add(evidence.latestAttemptId());
            }
            if (evidence.serviceName() != null) {
                if (evidence.applicableAttemptId() != null) {
                    evidenceIds.add(evidence.applicableAttemptId());
                }
                String serviceLine = "Observed service: " + normalize(evidence.serviceName(), MAXIMUM_SERVICE_LENGTH);
                String errorsLine = "Observed errors: "
                        + truncate(normalizeErrorCounts(evidence.errorCounts()), MAXIMUM_ERRORS_LENGTH);
                lines.add("Description: "
                        + boundedDescription(context.incidentDescription(), statusLine, serviceLine, errorsLine));
                lines.add(statusLine);
                lines.add(serviceLine);
                lines.add(errorsLine);
            } else {
                addIncidentOnlyQuery(lines, context, statusLine);
            }
        }

        return new DerivedKnowledgeQuery(String.join("\n", lines), TEMPLATE_VERSION, List.copyOf(evidenceIds));
    }

    private static void addIncidentOnlyQuery(List<String> lines, KnowledgeRetrievalContext context, String statusLine) {
        String titleLine = "Title: " + normalize(context.incidentTitle(), MAXIMUM_TITLE_LENGTH);
        lines.add(titleLine);
        lines.add("Description: " + boundedDescription(context.incidentDescription(), titleLine, statusLine));
        lines.add(statusLine);
    }

    private static String boundedDescription(String value, String... otherLines) {
        int fixedLength = "Description: ".length() + otherLines.length;
        for (String line : otherLines) {
            fixedLength += line.length();
        }
        return normalize(value, Math.max(1, MAXIMUM_QUERY_LENGTH - fixedLength));
    }

    private static String normalizeErrorCounts(List<KnowledgeErrorCount> errorCounts) {
        return errorCounts.stream()
                .map(error -> normalize(error.errorCode(), MAXIMUM_ERRORS_LENGTH) + " count " + error.count())
                .reduce((left, right) -> left + "; " + right)
                .orElse("none returned");
    }

    private static String normalize(String value, int maximumLength) {
        if (value == null) {
            return "unavailable";
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        return truncate(normalized.isEmpty() ? "unavailable" : normalized, maximumLength);
    }

    private static String truncate(String value, int maximumLength) {
        return value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }
}
