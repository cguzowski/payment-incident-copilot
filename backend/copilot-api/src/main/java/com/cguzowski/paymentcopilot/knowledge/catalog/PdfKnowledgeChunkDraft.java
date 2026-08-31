package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.UUID;

record PdfKnowledgeChunkDraft(
        UUID chunkId,
        int ordinal,
        String sectionPath,
        String rawContent,
        String embeddingInput,
        String rawContentHash,
        String embeddingInputHash,
        String embeddingInputTemplateVersion,
        String chunkingStrategyVersion,
        int pageNumber,
        int startBlock,
        int endBlock,
        int estimatedTokens) {}
