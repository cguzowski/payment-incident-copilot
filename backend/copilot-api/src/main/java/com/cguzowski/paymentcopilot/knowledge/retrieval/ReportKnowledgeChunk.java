package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.UUID;

public record ReportKnowledgeChunk(
        UUID chunkId,
        UUID documentId,
        String documentType,
        String documentTitle,
        String documentVersion,
        String sectionPath,
        String rawContent) {}
