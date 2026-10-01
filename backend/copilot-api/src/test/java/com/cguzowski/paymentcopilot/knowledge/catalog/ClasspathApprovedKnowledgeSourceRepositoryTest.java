package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class ClasspathApprovedKnowledgeSourceRepositoryTest {

    @Test
    void loadsApprovedGuidanceForEachAdditionalFamily() {
        var repository = new ClasspathApprovedKnowledgeSourceRepository(
                new DefaultResourceLoader(), new MarkdownKnowledgeDocumentParser());
        var documents = repository.findAll();
        for (String family : List.of(
                "AUTHORIZATION_TIMEOUT_SPIKE",
                "CAPTURE_FAILURE_SPIKE",
                "REFUND_FAILURE_SPIKE",
                "SETTLEMENT_DELAY",
                "WEBHOOK_DELIVERY_FAILURE",
                "RECONCILIATION_MISMATCH")) {
            assertThat(documents.stream()
                            .filter(document -> document.incidentFamily().equals(family))
                            .toList())
                    .hasSize(2)
                    .extracting(ApprovedKnowledgeDocument::type)
                    .containsExactlyInAnyOrder(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY);
        }
    }

    @Test
    void loadsOneApprovedRunbookAndPolicyForTheSyntheticIncidentFamily() {
        ClasspathApprovedKnowledgeSourceRepository repository = new ClasspathApprovedKnowledgeSourceRepository(
                new DefaultResourceLoader(), new MarkdownKnowledgeDocumentParser());

        List<ApprovedKnowledgeDocument> documents = repository.findAll().stream()
                .filter(document -> document.incidentFamily().equals("AUTHORIZATION_DECLINE_RATE_SPIKE"))
                .toList();

        assertThat(documents).hasSize(2);
        assertThat(documents)
                .extracting(ApprovedKnowledgeDocument::type)
                .containsExactly(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY);
        assertThat(documents).allSatisfy(document -> {
            assertThat(document.approvalStatus()).isEqualTo(KnowledgeApprovalStatus.APPROVED);
            assertThat(document.incidentFamily()).isEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
            assertThat(document.appliesTo()).isEqualTo("Card authorization");
            assertThat(document.body()).contains("GATEWAY_TIMEOUT", "UPSTREAM_CONNECTION_RESET");
        });
    }
}
