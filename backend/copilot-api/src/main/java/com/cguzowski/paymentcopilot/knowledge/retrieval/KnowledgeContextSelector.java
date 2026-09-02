package com.cguzowski.paymentcopilot.knowledge.retrieval;

import com.cguzowski.paymentcopilot.knowledge.catalog.KnowledgeDocumentType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class KnowledgeContextSelector {

    private final int maximumRunbooks;
    private final int maximumPolicies;

    KnowledgeContextSelector() {
        this(4, 3);
    }

    KnowledgeContextSelector(int maximumRunbooks, int maximumPolicies) {
        if (maximumRunbooks < 0 || maximumPolicies < 0) {
            throw new IllegalArgumentException("Knowledge context limits cannot be negative.");
        }
        this.maximumRunbooks = maximumRunbooks;
        this.maximumPolicies = maximumPolicies;
    }

    List<SelectedKnowledgeChunk> select(List<KnowledgeSearchCandidate> candidates) {
        List<SelectedKnowledgeChunk> selected = new ArrayList<>();
        Set<Integer> selectedCandidateIndexes = new HashSet<>();
        Set<UUID> selectedDocumentVersions = new HashSet<>();
        Map<KnowledgeDocumentType, Integer> selectedByType = new EnumMap<>(KnowledgeDocumentType.class);
        selectedByType.put(KnowledgeDocumentType.RUNBOOK, 0);
        selectedByType.put(KnowledgeDocumentType.POLICY, 0);

        selectPass(candidates, selected, selectedCandidateIndexes, selectedDocumentVersions, selectedByType, true);
        selectPass(candidates, selected, selectedCandidateIndexes, selectedDocumentVersions, selectedByType, false);
        return List.copyOf(selected);
    }

    private void selectPass(
            List<KnowledgeSearchCandidate> candidates,
            List<SelectedKnowledgeChunk> selected,
            Set<Integer> selectedCandidateIndexes,
            Set<UUID> selectedDocumentVersions,
            Map<KnowledgeDocumentType, Integer> selectedByType,
            boolean distinctDocumentsOnly) {
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            KnowledgeSearchCandidate candidate = candidates.get(candidateIndex);
            if (selectedCandidateIndexes.contains(candidateIndex)
                    || !hasCapacity(candidate.documentType(), selectedByType)
                    || (distinctDocumentsOnly && selectedDocumentVersions.contains(candidate.documentVersionId()))) {
                continue;
            }
            selectedCandidateIndexes.add(candidateIndex);
            selectedDocumentVersions.add(candidate.documentVersionId());
            selectedByType.compute(candidate.documentType(), (type, count) -> count + 1);
            selected.add(new SelectedKnowledgeChunk(candidate, candidateIndex + 1, selected.size() + 1));
        }
    }

    private boolean hasCapacity(
            KnowledgeDocumentType documentType, Map<KnowledgeDocumentType, Integer> selectedByType) {
        int maximum = documentType == KnowledgeDocumentType.RUNBOOK ? maximumRunbooks : maximumPolicies;
        return selectedByType.get(documentType) < maximum;
    }
}
