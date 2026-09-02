package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.util.UUID;

record EvaluationCandidateResult(
        String documentKey,
        UUID documentId,
        String documentVersion,
        UUID documentVersionId,
        UUID chunkId,
        int chunkOrdinal,
        String documentType,
        boolean eligible,
        Float lexicalRank,
        Integer lexicalPosition,
        Float vectorSimilarity,
        Integer vectorPosition,
        int fusedPosition,
        double fusedScore,
        Integer selectedPosition,
        String sourceName,
        String sourceFormat,
        String pdfSha256,
        Integer sourceStartPage,
        Integer sourceEndPage,
        Integer sourceStartBlock,
        Integer sourceEndBlock) {

    EvaluationCandidateResult withRanks(Float lexicalRank, Float vectorSimilarity) {
        return copy(
                lexicalRank,
                lexicalRank == null ? null : lexicalPosition,
                vectorSimilarity,
                vectorSimilarity == null ? null : vectorPosition,
                chunkId,
                eligible,
                selectedPosition);
    }

    EvaluationCandidateResult withChunkId(UUID chunkId) {
        return copy(
                lexicalRank, lexicalPosition, vectorSimilarity, vectorPosition, chunkId, eligible, selectedPosition);
    }

    EvaluationCandidateResult withEligible(boolean eligible) {
        return copy(
                lexicalRank, lexicalPosition, vectorSimilarity, vectorPosition, chunkId, eligible, selectedPosition);
    }

    EvaluationCandidateResult withSelectedPosition(Integer selectedPosition) {
        return copy(
                lexicalRank, lexicalPosition, vectorSimilarity, vectorPosition, chunkId, eligible, selectedPosition);
    }

    private EvaluationCandidateResult copy(
            Float lexicalRank,
            Integer lexicalPosition,
            Float vectorSimilarity,
            Integer vectorPosition,
            UUID chunkId,
            boolean eligible,
            Integer selectedPosition) {
        return new EvaluationCandidateResult(
                documentKey,
                documentId,
                documentVersion,
                documentVersionId,
                chunkId,
                chunkOrdinal,
                documentType,
                eligible,
                lexicalRank,
                lexicalPosition,
                vectorSimilarity,
                vectorPosition,
                fusedPosition,
                fusedScore,
                selectedPosition,
                sourceName,
                sourceFormat,
                pdfSha256,
                sourceStartPage,
                sourceEndPage,
                sourceStartBlock,
                sourceEndBlock);
    }
}
