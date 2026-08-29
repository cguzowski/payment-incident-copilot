package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.List;

interface KnowledgeSearchRepository {
    List<KnowledgeSearchCandidate> search(KnowledgeSearchRequest request);
}
