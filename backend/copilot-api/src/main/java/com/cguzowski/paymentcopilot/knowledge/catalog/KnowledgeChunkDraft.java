package com.cguzowski.paymentcopilot.knowledge.catalog;

record KnowledgeChunkDraft(
        int ordinal,
        String sectionPath,
        String rawContent,
        String embeddingInput,
        String rawContentHash,
        String embeddingInputHash,
        String embeddingInputTemplateVersion,
        int sourceStartLine,
        int sourceEndLine,
        int estimatedTokens) {}
