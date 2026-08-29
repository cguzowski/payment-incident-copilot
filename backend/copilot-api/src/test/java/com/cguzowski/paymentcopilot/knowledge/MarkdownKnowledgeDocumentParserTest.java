package com.cguzowski.paymentcopilot.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarkdownKnowledgeDocumentParserTest {

    private static final String MARKDOWN = """
            ---
            documentId: 66a84fed-3d77-4e7e-9a1b-e25ff37e2280
            tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61
            type: RUNBOOK
            title: Authorization Decline Runbook
            version: 1.0.0
            incidentFamily: AUTHORIZATION_DECLINE_RATE_SPIKE
            appliesTo: Card authorization
            approvalStatus: APPROVED
            approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1
            approvedAt: 2026-08-20T10:00:00Z
            effectiveAt: 2026-08-21T00:00:00Z
            ---
            # Authorization Decline Runbook

            ## Gateway Failures

            Inspect timeout and connection-reset observations.
            """;

    @Test
    void parsesApprovedRunbookMetadataAndPreservesBodyExactly() {
        ApprovedKnowledgeDocument document = new MarkdownKnowledgeDocumentParser()
                .parse("authorization-decline-runbook.md", MARKDOWN);

        assertThat(document.documentId())
                .isEqualTo(UUID.fromString("66a84fed-3d77-4e7e-9a1b-e25ff37e2280"));
        assertThat(document.tenantId())
                .isEqualTo(UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61"));
        assertThat(document.type()).isEqualTo(KnowledgeDocumentType.RUNBOOK);
        assertThat(document.title()).isEqualTo("Authorization Decline Runbook");
        assertThat(document.version()).isEqualTo("1.0.0");
        assertThat(document.incidentFamily()).isEqualTo("AUTHORIZATION_DECLINE_RATE_SPIKE");
        assertThat(document.appliesTo()).isEqualTo("Card authorization");
        assertThat(document.approvalStatus()).isEqualTo(KnowledgeApprovalStatus.APPROVED);
        assertThat(document.approvedBy())
                .isEqualTo(UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"));
        assertThat(document.approvedAt()).isEqualTo(Instant.parse("2026-08-20T10:00:00Z"));
        assertThat(document.effectiveAt()).isEqualTo(Instant.parse("2026-08-21T00:00:00Z"));
        assertThat(document.sourceName()).isEqualTo("authorization-decline-runbook.md");
        assertThat(document.bodyStartLine()).isEqualTo(14);
        assertThat(document.body()).isEqualTo("""
                # Authorization Decline Runbook

                ## Gateway Failures

                Inspect timeout and connection-reset observations.
                """);
    }

    @Test
    void rejectsUnknownUnboundedOrMalformedSourceMetadata() {
        MarkdownKnowledgeDocumentParser parser = new MarkdownKnowledgeDocumentParser();

        assertThatThrownBy(() -> parser.parse(
                        "fixture.md",
                        MARKDOWN.replace("type: RUNBOOK", "type: UNTRUSTED")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(
                        "fixture.md",
                        MARKDOWN.replace(
                                "title: Authorization Decline Runbook",
                                "title: " + "x".repeat(161))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(
                        "fixture.md",
                        MARKDOWN.replace("approvedBy: 7b636625-53d1-46f7-92a9-9c8c27a243d1\n", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
