package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
final class MarkdownKnowledgeDocumentParser {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "documentId",
            "tenantId",
            "type",
            "title",
            "version",
            "incidentFamily",
            "appliesTo",
            "approvalStatus",
            "approvedBy",
            "approvedAt",
            "effectiveAt");

    ApprovedKnowledgeDocument parse(String sourceName, String markdown) {
        if (sourceName == null || sourceName.isBlank() || markdown == null || !markdown.startsWith("---\n")) {
            throw new IllegalArgumentException("Knowledge document must start with valid front matter.");
        }
        int closingDelimiter = markdown.indexOf("\n---\n", 4);
        if (closingDelimiter < 0) {
            throw new IllegalArgumentException("Knowledge document front matter is not closed.");
        }

        Map<String, String> metadata = parseMetadata(markdown.substring(4, closingDelimiter));
        if (!metadata.keySet().equals(REQUIRED_FIELDS)) {
            throw new IllegalArgumentException("Knowledge document metadata fields are invalid.");
        }

        String body = markdown.substring(closingDelimiter + 5);
        if (body.isBlank()) {
            throw new IllegalArgumentException("Knowledge document body is required.");
        }
        int bodyStartLine = countLines(markdown.substring(0, closingDelimiter + 5)) + 1;
        return new ApprovedKnowledgeDocument(
                uuid(metadata, "documentId"),
                uuid(metadata, "tenantId"),
                enumValue(KnowledgeDocumentType.class, metadata, "type"),
                bounded(metadata, "title", 160),
                bounded(metadata, "version", 40),
                bounded(metadata, "incidentFamily", 80),
                bounded(metadata, "appliesTo", 120),
                enumValue(KnowledgeApprovalStatus.class, metadata, "approvalStatus"),
                uuid(metadata, "approvedBy"),
                instant(metadata, "approvedAt"),
                instant(metadata, "effectiveAt"),
                sourceName,
                bodyStartLine,
                body);
    }

    private static Map<String, String> parseMetadata(String frontMatter) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : frontMatter.split("\n", -1)) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Knowledge document metadata is malformed.");
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (key.isBlank() || value.isBlank() || values.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Knowledge document metadata is malformed.");
            }
        }
        return values;
    }

    private static UUID uuid(Map<String, String> metadata, String field) {
        try {
            return UUID.fromString(metadata.get(field));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Knowledge document " + field + " is invalid.", exception);
        }
    }

    private static Instant instant(Map<String, String> metadata, String field) {
        try {
            return Instant.parse(metadata.get(field));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Knowledge document " + field + " is invalid.", exception);
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, Map<String, String> metadata, String field) {
        try {
            return Enum.valueOf(type, metadata.get(field));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Knowledge document " + field + " is invalid.", exception);
        }
    }

    private static String bounded(Map<String, String> metadata, String field, int maximumLength) {
        String value = metadata.get(field);
        if (value.length() > maximumLength || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Knowledge document " + field + " is invalid.");
        }
        return value;
    }

    private static int countLines(String value) {
        return Math.toIntExact(value.lines().count());
    }
}
