package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;

interface ApprovedKnowledgeSourceRepository {
    List<ApprovedKnowledgeDocument> findAll();
}
