package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record SynTenRetrievalEvaluationRun(
        String schemaVersion,
        String runId,
        String evaluationVersion,
        String corpusVersion,
        String catalogFingerprint,
        String extractionVersion,
        String chunkingVersion,
        String embeddingModel,
        int embeddingDimensions,
        Instant evaluatedAt,
        Instant completedAt,
        UUID tenantId,
        EvaluationThresholds thresholds,
        List<SynTenRetrievalEvaluationVariantRecord> variants,
        SynTenRetrievalEvaluationGrade grade) {

    static final String SCHEMA_VERSION = "synten-retrieval-eval-result/v1";

    SynTenRetrievalEvaluationRun {
        variants = List.copyOf(variants);
    }
}

record EvaluationThresholds(
        int primaryRunbookRequired,
        int supportingPolicyRequired,
        int supportingPolicyApplicable,
        int primaryOutranksWeakPercentRequired,
        int ineligibleCandidateMaximum,
        boolean partialSemanticsRequired,
        boolean unavailableSemanticsRequired,
        boolean supersededExclusionRequired) {}

record SynTenRetrievalEvaluationVariantRecord(
        String caseId,
        String variantId,
        String scenarioReference,
        UUID incidentId,
        UUID investigationId,
        UUID evidenceId,
        List<EvaluationEvidenceError> evidenceErrors,
        List<UUID> contributingEvidenceIds,
        EvaluationFilterResult filters,
        QueryEmbeddingOutcome queryEmbedding,
        EvaluationVariantResult result,
        EvaluationVariantGrade assertions) {

    SynTenRetrievalEvaluationVariantRecord {
        evidenceErrors = List.copyOf(evidenceErrors);
        contributingEvidenceIds = List.copyOf(contributingEvidenceIds);
    }
}

record EvaluationEvidenceError(String errorCode, int count) {}

record EvaluationFilterResult(
        UUID tenantId, String incidentFamily, List<String> documentTypes, String approvalStatus, Instant effectiveAt) {

    EvaluationFilterResult {
        documentTypes = List.copyOf(documentTypes);
    }
}
