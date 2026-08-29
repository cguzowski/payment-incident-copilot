package com.cguzowski.paymentcopilot.knowledge;

import java.util.ArrayList;
import java.util.List;
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
        int runbooks = 0;
        int policies = 0;
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            KnowledgeSearchCandidate candidate = candidates.get(candidateIndex);
            boolean include = switch (candidate.documentType()) {
                case RUNBOOK -> runbooks < maximumRunbooks;
                case POLICY -> policies < maximumPolicies;
            };
            if (!include) {
                continue;
            }
            if (candidate.documentType() == KnowledgeDocumentType.RUNBOOK) {
                runbooks++;
            } else {
                policies++;
            }
            selected.add(new SelectedKnowledgeChunk(candidate, candidateIndex + 1, selected.size() + 1));
        }
        return List.copyOf(selected);
    }
}
