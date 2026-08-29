package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class KnowledgeRetrievalQueryBuilder {

    static final String TEMPLATE_VERSION = "knowledge-query/v1";
    static final int MAXIMUM_QUERY_LENGTH = 2000;

    DerivedKnowledgeQuery build(KnowledgeRetrievalContext context) {
        List<String> lines = new ArrayList<>();
        lines.add("Incident type: " + context.incidentFamily());
        lines.add("Title: " + normalize(context.incidentTitle()));
        lines.add("Description: " + normalize(context.incidentDescription()));

        LinkedHashSet<UUID> evidenceIds = new LinkedHashSet<>();
        KnowledgeEvidenceReference evidence = context.evidence();
        if (evidence == null) {
            lines.add("Observed evidence status: NOT_COLLECTED");
        } else {
            if (evidence.latestAttemptId() != null) {
                evidenceIds.add(evidence.latestAttemptId());
            }
            lines.add("Observed evidence status: "
                    + (evidence.latestStatus() == null ? "UNKNOWN" : evidence.latestStatus()));
            if (evidence.serviceName() != null) {
                if (evidence.applicableAttemptId() != null) {
                    evidenceIds.add(evidence.applicableAttemptId());
                }
                lines.add("Observed service: " + normalize(evidence.serviceName()));
                lines.add("Observed errors: " + normalizeErrorCounts(evidence.errorCounts()));
            }
        }

        String text = String.join("\n", lines);
        if (text.length() > MAXIMUM_QUERY_LENGTH) {
            text = text.substring(0, MAXIMUM_QUERY_LENGTH);
        }
        return new DerivedKnowledgeQuery(text, TEMPLATE_VERSION, List.copyOf(evidenceIds));
    }

    private static String normalizeErrorCounts(List<KnowledgeErrorCount> errorCounts) {
        return errorCounts.stream()
                .map(error -> normalize(error.errorCode()) + " count " + error.count())
                .reduce((left, right) -> left + "; " + right)
                .orElse("none returned");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "unavailable";
        }
        String normalized = value.strip().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? "unavailable" : normalized;
    }
}
