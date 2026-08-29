package com.cguzowski.paymentcopilot.knowledge;

import java.util.List;

interface KnowledgeSearchRepository {
    List<KnowledgeSearchCandidate> search(KnowledgeSearchRequest request);
}
