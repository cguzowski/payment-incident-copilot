package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

record SynTenRetrievalEvaluationSeed(
        String schemaVersion,
        String evaluationVersion,
        String corpusVersion,
        String runId,
        Instant createdAt,
        Instant evaluatedAt,
        UUID tenantId,
        String evaluationDatabaseName,
        List<SynTenRetrievalEvaluationSeedMapping> mappings) {

    static final String SCHEMA_VERSION = "synten-retrieval-eval-seed/v1";

    SynTenRetrievalEvaluationSeed {
        mappings = List.copyOf(mappings);
    }

    void validateAgainst(SynTenRetrievalEvaluationContract contract) {
        Objects.requireNonNull(contract, "contract");
        if (!SCHEMA_VERSION.equals(schemaVersion)
                || !contract.evaluationVersion().equals(evaluationVersion)
                || !contract.corpusVersion().equals(corpusVersion)
                || !contract.evaluatedAt().equals(evaluatedAt)) {
            throw invalid("Evaluation seed does not match the reviewed evaluation contract.");
        }
        if (!SynTenRetrievalEvaluationContractRepository.TENANT_ID.equals(tenantId)) {
            throw invalid("Evaluation seed tenant does not match the synthetic SynTen tenant.");
        }
        if (runId == null || !runId.matches("[0-9a-f]{32}") || createdAt == null || createdAt.isBefore(evaluatedAt)) {
            throw invalid("Evaluation seed run metadata is malformed.");
        }
        if (evaluationDatabaseName == null
                || !evaluationDatabaseName.matches("payment_copilot_k4_eval(?:_[a-z0-9_]+)?")) {
            throw invalid("Evaluation seed must name a dedicated K4 evaluation database.");
        }
        Map<VariantKey, SynTenRetrievalEvaluationSeedMapping> actual = new LinkedHashMap<>();
        for (SynTenRetrievalEvaluationSeedMapping mapping : mappings) {
            validateMappingShape(mapping);
            VariantKey key = new VariantKey(mapping.caseId(), mapping.variantId());
            if (actual.putIfAbsent(key, mapping) != null) {
                throw invalid("Evaluation seed contains a duplicate mapping: " + key + ".");
            }
        }
        if (mappings.size() != 37) {
            throw invalid("Evaluation seed must contain exactly 37 mappings.");
        }
        Set<VariantKey> expected = expectedVariants(contract);
        if (!actual.keySet().equals(expected)) {
            throw invalid("Evaluation seed mapping coverage differs from the reviewed contract.");
        }
        for (VariantKey key : expected) {
            validateEvidence(contract, actual.get(key));
        }
    }

    private static void validateMappingShape(SynTenRetrievalEvaluationSeedMapping mapping) {
        if (mapping == null
                || mapping.caseId() == null
                || !mapping.caseId().matches("KQ-\\d{3}")
                || mapping.variantId() == null
                || mapping.scenarioReference() == null
                || !mapping.scenarioReference().matches("sig-v1-S\\d{3}-\\d{10}-[0-9a-f]{12}")
                || mapping.incidentId() == null
                || mapping.investigationId() == null
                || mapping.evidenceId() == null
                || mapping.evidenceStatus() == null) {
            throw invalid("Evaluation seed mapping is malformed.");
        }
        String scenarioCode = mapping.scenarioReference().substring("sig-v1-".length(), "sig-v1-S000".length());
        String expectedCode = mapping.variantId().equals("EXCLUSION") ? "S999" : mapping.variantId();
        if (!scenarioCode.equals(expectedCode)) {
            throw invalid("Evaluation seed mapping scenario reference does not match its variant.");
        }
    }

    private static Set<VariantKey> expectedVariants(SynTenRetrievalEvaluationContract contract) {
        Set<VariantKey> expected = new java.util.LinkedHashSet<>();
        for (RetrievalEvaluationCase evaluationCase : contract.cases()) {
            List<String> variants =
                    evaluationCase.caseId().equals("KQ-023") ? List.of("EXCLUSION") : evaluationCase.scenarioIds();
            for (String variant : variants) {
                expected.add(new VariantKey(evaluationCase.caseId(), variant));
            }
        }
        return expected;
    }

    private static void validateEvidence(
            SynTenRetrievalEvaluationContract contract, SynTenRetrievalEvaluationSeedMapping mapping) {
        String expected;
        if (mapping.caseId().equals("KQ-023")) {
            expected = "NOT_FOUND";
        } else {
            expected = contract.scenarios().get(mapping.variantId()).evidence().availability();
        }
        if (!expected.equals(mapping.evidenceStatus())) {
            throw invalid("Evaluation seed evidence status differs from its reviewed scenario.");
        }
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private record VariantKey(String caseId, String variantId) {
        @Override
        public String toString() {
            return caseId + "/" + variantId;
        }
    }
}

record SynTenRetrievalEvaluationSeedMapping(
        String caseId,
        String variantId,
        String scenarioReference,
        UUID incidentId,
        UUID investigationId,
        UUID evidenceId,
        String evidenceStatus) {}
