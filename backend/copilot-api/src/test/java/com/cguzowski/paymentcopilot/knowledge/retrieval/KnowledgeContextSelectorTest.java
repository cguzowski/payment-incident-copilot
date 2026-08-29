package com.cguzowski.paymentcopilot.knowledge.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeApprovalStatus;
import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeContextSelectorTest {

    @Test
    void selectsBoundedRunbookAndPolicyContextWithoutFillingFromWeakerTypes() {
        List<KnowledgeSearchCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            candidates.add(candidate(KnowledgeDocumentType.RUNBOOK, index, 1.0 - index / 100.0));
        }
        for (int index = 0; index < 5; index++) {
            candidates.add(candidate(KnowledgeDocumentType.POLICY, index + 10, 0.9 - index / 100.0));
        }

        List<SelectedKnowledgeChunk> selected = new KnowledgeContextSelector(4, 3).select(candidates);

        assertThat(selected).hasSize(7);
        assertThat(selected.stream().filter(item -> item.candidate().documentType() == KnowledgeDocumentType.RUNBOOK))
                .hasSize(4);
        assertThat(selected.stream().filter(item -> item.candidate().documentType() == KnowledgeDocumentType.POLICY))
                .hasSize(3);
        assertThat(selected)
                .extracting(SelectedKnowledgeChunk::selectedPosition)
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(selected)
                .extracting(item -> item.candidate().chunkId())
                .containsExactly(
                        candidates.get(0).chunkId(),
                        candidates.get(1).chunkId(),
                        candidates.get(2).chunkId(),
                        candidates.get(3).chunkId(),
                        candidates.get(6).chunkId(),
                        candidates.get(7).chunkId(),
                        candidates.get(8).chunkId());
    }

    @Test
    void returnsFewerWhenOneApprovedTypeHasNoEligibleCandidates() {
        List<KnowledgeSearchCandidate> candidates = List.of(
                candidate(KnowledgeDocumentType.RUNBOOK, 1, 0.9), candidate(KnowledgeDocumentType.RUNBOOK, 2, 0.8));

        List<SelectedKnowledgeChunk> selected = new KnowledgeContextSelector(4, 3).select(candidates);

        assertThat(selected).hasSize(2);
        assertThat(selected)
                .allSatisfy(
                        item -> assertThat(item.candidate().documentType()).isEqualTo(KnowledgeDocumentType.RUNBOOK));
    }

    private static KnowledgeSearchCandidate candidate(KnowledgeDocumentType type, int ordinal, double score) {
        UUID chunkId = UUID.nameUUIDFromBytes((type + "-chunk-" + ordinal).getBytes());
        return new KnowledgeSearchCandidate(
                UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61"),
                chunkId,
                UUID.nameUUIDFromBytes((type + "-version-" + ordinal).getBytes()),
                UUID.nameUUIDFromBytes((type + "-document-" + ordinal).getBytes()),
                type,
                type + " fixture",
                "1.0.0",
                "AUTHORIZATION_DECLINE_RATE_SPIKE",
                "Card authorization",
                "Diagnosis",
                "Synthetic approved guidance " + ordinal,
                10,
                12,
                KnowledgeApprovalStatus.APPROVED,
                UUID.fromString("7b636625-53d1-46f7-92a9-9c8c27a243d1"),
                Instant.parse("2026-08-20T10:00:00Z"),
                Instant.parse("2026-08-21T00:00:00Z"),
                0.2f,
                ordinal + 1,
                0.9f,
                ordinal + 1,
                score);
    }
}
