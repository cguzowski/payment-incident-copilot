package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeSourceHasherTest {

    @Test
    void changesHashWhenCitationLineNumbersChange() {
        ApprovedKnowledgeDocument original = documentAtLine(14);
        ApprovedKnowledgeDocument shifted = documentAtLine(15);

        assertThat(KnowledgeSourceHasher.hash(shifted))
                .isNotEqualTo(KnowledgeSourceHasher.hash(original));
    }

    private static ApprovedKnowledgeDocument documentAtLine(int bodyStartLine) {
        return new ApprovedKnowledgeDocument(
                UUID.fromString("66a84fed-3d77-4e7e-9a1b-e25ff37e2280"),
                UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61"),
                KnowledgeDocumentType.RUNBOOK,
                "Authorization Decline Runbook",
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-20T10:00:00Z"),
                Instant.parse("2026-08-21T00:00:00Z"),
                "authorization-decline-runbook.md",
                bodyStartLine,
                "# Authorization Decline Runbook\n\n## Diagnosis\n\nExact source paragraph.\n");
    }
}
