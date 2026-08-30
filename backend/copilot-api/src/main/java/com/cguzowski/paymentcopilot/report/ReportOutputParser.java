package com.cguzowski.paymentcopilot.report;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
class ReportOutputParser {

    private static final int MAX_RAW_OUTPUT_LENGTH = 40_000;

    private final JsonMapper jsonMapper;
    private final Schema schema;
    private final ReportDocumentValidator validator;

    ReportOutputParser(JsonMapper jsonMapper, ReportPromptFactory prompts) {
        this.jsonMapper = jsonMapper;
        this.schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(prompts.schema());
        this.validator = new ReportDocumentValidator();
    }

    ReportDocument parse(String rawOutput, ReportGenerationContext context) {
        if (rawOutput == null || rawOutput.isBlank() || rawOutput.length() > MAX_RAW_OUTPUT_LENGTH) {
            throw invalid();
        }
        String json = rawOutput.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw invalid();
        }
        try {
            JsonNode node = jsonMapper.readTree(json);
            if (!schema.validate(node).isEmpty()) {
                throw invalid();
            }
            ReportDocument document = jsonMapper.treeToValue(node, ReportDocument.class);
            validator.validate(document, validationContext(context));
            return document;
        } catch (JacksonException exception) {
            throw invalid();
        }
    }

    private static ReportValidationContext validationContext(ReportGenerationContext context) {
        Set<UUID> evidenceIds = new HashSet<>();
        evidenceIds.add(context.evidence().latestAttemptId());
        if (context.evidence().applicableAttemptId() != null) {
            evidenceIds.add(context.evidence().applicableAttemptId());
        }
        Set<UUID> knowledgeIds = new HashSet<>();
        context.knowledge().chunks().forEach(chunk -> knowledgeIds.add(chunk.chunkId()));
        return new ReportValidationContext(evidenceIds, knowledgeIds);
    }

    private static InvalidReportDocumentException invalid() {
        return new InvalidReportDocumentException("The model response did not match report-v1.");
    }
}
