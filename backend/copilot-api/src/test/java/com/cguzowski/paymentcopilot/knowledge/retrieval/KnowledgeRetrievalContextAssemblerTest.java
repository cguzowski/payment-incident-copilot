package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cguzowski.paymentcopilot.evidence.EvidenceErrorCount;
import com.cguzowski.paymentcopilot.evidence.EvidenceSnapshot;
import com.cguzowski.paymentcopilot.evidence.EvidenceSnapshotProvider;
import com.cguzowski.paymentcopilot.incident.InvestigationSnapshot;
import com.cguzowski.paymentcopilot.incident.InvestigationSnapshotProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeRetrievalContextAssemblerTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private static final UUID INVESTIGATION_ID = UUID.fromString("7f50162a-8dc5-45b0-9c88-dc2f77135e0f");
    private static final UUID CORRELATION_ID = UUID.fromString("133767cf-8ec8-487d-a09d-19d5efcece07");
    private static final UUID LATEST_EVIDENCE_ID = UUID.fromString("5b8e57e4-e194-4f51-81dc-4d2e6e47103a");
    private static final UUID APPLICABLE_EVIDENCE_ID = UUID.fromString("d3a12cd9-ef1d-4328-b63d-2ecea6558e2d");

    @Test
    void composesIncidentAndNormalizedEvidenceSnapshots() {
        InvestigationSnapshotProvider investigations = mock(InvestigationSnapshotProvider.class);
        EvidenceSnapshotProvider evidence = mock(EvidenceSnapshotProvider.class);
        when(investigations.findKnowledgeRetrievalSnapshot(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(new InvestigationSnapshot(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        CORRELATION_ID,
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Authorization declines elevated",
                        "Synthetic authorization declines exceeded the observation threshold.")));
        when(evidence.findByTenantIdAndInvestigationId(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.of(new EvidenceSnapshot(
                        LATEST_EVIDENCE_ID,
                        "UNAVAILABLE",
                        APPLICABLE_EVIDENCE_ID,
                        "authorization-gateway",
                        List.of(new EvidenceErrorCount("GATEWAY_TIMEOUT", 12)))));

        Optional<KnowledgeRetrievalContext> result =
                new KnowledgeRetrievalContextAssembler(investigations, evidence).find(TENANT_ID, INVESTIGATION_ID);

        assertThat(result)
                .contains(new KnowledgeRetrievalContext(
                        TENANT_ID,
                        INVESTIGATION_ID,
                        CORRELATION_ID,
                        "AUTHORIZATION_DECLINE_RATE_SPIKE",
                        "Authorization declines elevated",
                        "Synthetic authorization declines exceeded the observation threshold.",
                        new KnowledgeEvidenceReference(
                                LATEST_EVIDENCE_ID,
                                "UNAVAILABLE",
                                APPLICABLE_EVIDENCE_ID,
                                "authorization-gateway",
                                List.of(new KnowledgeErrorCount("GATEWAY_TIMEOUT", 12)))));
    }

    @Test
    void stopsBeforeEvidenceLookupWhenTheTenantDoesNotOwnTheInvestigation() {
        InvestigationSnapshotProvider investigations = mock(InvestigationSnapshotProvider.class);
        EvidenceSnapshotProvider evidence = mock(EvidenceSnapshotProvider.class);
        when(investigations.findKnowledgeRetrievalSnapshot(TENANT_ID, INVESTIGATION_ID))
                .thenReturn(Optional.empty());

        assertThat(new KnowledgeRetrievalContextAssembler(investigations, evidence).find(TENANT_ID, INVESTIGATION_ID))
                .isEmpty();
        verify(evidence, never()).findByTenantIdAndInvestigationId(TENANT_ID, INVESTIGATION_ID);
    }
}
