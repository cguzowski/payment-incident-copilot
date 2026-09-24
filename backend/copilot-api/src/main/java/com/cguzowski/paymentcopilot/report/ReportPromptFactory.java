package com.cguzowski.paymentcopilot.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
class ReportPromptFactory {

    static final String PROMPT_VERSION = "report-prompt/v3";
    static final String SCHEMA_VERSION = "report-v1";

    private final JsonMapper jsonMapper;
    private final String template;
    private final String schema;

    ReportPromptFactory(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.template = read("prompts/report-v1.txt");
        this.schema = read("reports/report-v1.schema.json");
    }

    ReportPrompt build(ReportGenerationContext context) {
        try {
            String input = jsonMapper.writeValueAsString(context);
            String text = template.replace("{{SCHEMA}}", schema).replace("{{INPUT}}", input);
            String outputSchema = constrainedSchema(context);
            return new ReportPrompt(
                    text, PROMPT_VERSION, sha256(template), SCHEMA_VERSION, sha256(outputSchema), outputSchema);
        } catch (JacksonException exception) {
            throw new IllegalStateException("The validated report context could not be serialized.", exception);
        }
    }

    String schema() {
        return schema;
    }

    private String constrainedSchema(ReportGenerationContext context) throws JacksonException {
        ObjectNode root = (ObjectNode) jsonMapper.readTree(schema);
        ObjectNode definitions = (ObjectNode) root.get("$defs");
        Set<UUID> evidenceIds = new LinkedHashSet<>();
        evidenceIds.add(context.evidence().latestAttemptId());
        if (context.evidence().applicableAttemptId() != null) {
            evidenceIds.add(context.evidence().applicableAttemptId());
        }
        replaceIdentifierDefinition(definitions, "evidenceId", evidenceIds);
        replaceIdentifierDefinition(
                definitions,
                "knowledgeChunkId",
                context.knowledge().chunks().stream()
                        .map(chunk -> chunk.chunkId())
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        return jsonMapper.writeValueAsString(root);
    }

    private void replaceIdentifierDefinition(ObjectNode definitions, String name, Set<UUID> identifiers) {
        ObjectNode definition = jsonMapper.createObjectNode();
        definition.put("type", "string");
        definition.put("format", "uuid");
        ArrayNode values = definition.putArray("enum");
        identifiers.forEach(identifier -> values.add(identifier.toString()));
        definitions.set(name, definition);
    }

    private static String read(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Required report resource is unavailable: " + path, exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
