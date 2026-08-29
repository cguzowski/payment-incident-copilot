package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class ClasspathApprovedKnowledgeSourceRepositoryTest {

    @Test
    void loadsOneApprovedRunbookAndPolicyForTheSyntheticIncidentFamily() {
        ClasspathApprovedKnowledgeSourceRepository repository = new ClasspathApprovedKnowledgeSourceRepository(
                new DefaultResourceLoader(), new MarkdownKnowledgeDocumentParser());

        List<ApprovedKnowledgeDocument> documents = repository.findAll();

        assertThat(documents).hasSize(2);
        assertThat(documents).extracting(ApprovedKnowledgeDocument::type)
                .containsExactly(KnowledgeDocumentType.RUNBOOK, KnowledgeDocumentType.POLICY);
        assertThat(documents).allSatisfy(document -> {
            assertThat(document.approvalStatus()).isEqualTo(KnowledgeApprovalStatus.APPROVED);
            assertThat(document.incidentFamily()).isEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
            assertThat(document.appliesTo()).isEqualTo("Card authorization");
            assertThat(document.body()).contains("GATEWAY_TIMEOUT", "UPSTREAM_CONNECTION_RESET");
        });
    }
}
