package com.cguzowski.paymentcopilot.knowledge;

import java.util.List;

interface ApprovedKnowledgeSourceRepository {
    List<ApprovedKnowledgeDocument> findAll();
}
