package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.incident.EvidenceCollectionStatus;
import com.cguzowski.paymentcopilot.incident.IncidentType;
import com.cguzowski.paymentcopilot.incident.ServiceErrorEvidenceContent;
import com.cguzowski.paymentcopilot.incident.ServiceErrorObservation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeRetrievalQueryBuilderTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");

    @Test
    void derivesBoundedQueryFromIncidentAndNormalizedApplicableEvidence() {
        ServiceErrorEvidenceContent content = new ServiceErrorEvidenceContent(
                "authorization-gateway",
                Instant.parse("2026-08-28T08:00:00Z"),
                Instant.parse("2026-08-28T08:05:00Z"),
                List.of(
                        new ServiceErrorObservation("evt-2", Instant.parse("2026-08-28T08:04:00Z"),
                                "UPSTREAM_CONNECTION_RESET", 4),
                        new ServiceErrorObservation("evt-1", Instant.parse("2026-08-28T08:03:00Z"),
                                "GATEWAY_TIMEOUT", 12)));
        KnowledgeRetrievalContext context = new KnowledgeRetrievalContext(
                TENANT_ID,
                INVESTIGATION_ID,
                CORRELATION_ID,
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                "Authorization declines elevated",
                "Synthetic authorization declines exceeded the observation threshold.",
                new KnowledgeEvidenceReference(
                        EVIDENCE_ID,
                        EvidenceCollectionStatus.AVAILABLE,
                        EVIDENCE_ID,
                        content));

        DerivedKnowledgeQuery query = new KnowledgeRetrievalQueryBuilder().build(context);

        assertThat(query.templateVersion()).isEqualTo("knowledge-query/v1");
        assertThat(query.contributingEvidenceIds()).containsExactly(EVIDENCE_ID);
        assertThat(query.text()).isEqualTo("""
                Incident type: AUTHORIZATION_DECLINE_RATE_SPIKE
                Title: Authorization declines elevated
                Description: Synthetic authorization declines exceeded the observation threshold.
                Observed evidence status: AVAILABLE
                Observed service: authorization-gateway
                Observed errors: GATEWAY_TIMEOUT count 12; UPSTREAM_CONNECTION_RESET count 4""");
        assertThat(query.text()).hasSizeLessThanOrEqualTo(2000);
    }

    @Test
    void preservesUnavailableEvidenceWithoutInventingObservations() {
        UUID unavailableId = UUID.fromString("5b8e57e4-e194-4f51-81dc-4d2e6e47103a");
        KnowledgeRetrievalContext context = new KnowledgeRetrievalContext(
                TENANT_ID,
                INVESTIGATION_ID,
                CORRELATION_ID,
                IncidentType.AUTHORIZATION_DECLINE_RATE_SPIKE,
                "Authorization declines elevated",
                "No service observations were returned.",
                new KnowledgeEvidenceReference(
                        unavailableId,
                        EvidenceCollectionStatus.UNAVAILABLE,
                        null,
                        null));

        DerivedKnowledgeQuery query = new KnowledgeRetrievalQueryBuilder().build(context);

        assertThat(query.text()).contains("Observed evidence status: UNAVAILABLE");
        assertThat(query.text()).doesNotContain("Observed service:", "Observed errors:");
        assertThat(query.contributingEvidenceIds()).containsExactly(unavailableId);
    }
}
