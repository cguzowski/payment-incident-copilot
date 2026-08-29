package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

@Repository
class ClasspathApprovedKnowledgeSourceRepository implements ApprovedKnowledgeSourceRepository {

    private static final List<String> SOURCES =
            List.of("authorization-decline-runbook.md", "payment-incident-response-policy.md");

    private final ResourceLoader resourceLoader;
    private final MarkdownKnowledgeDocumentParser parser;

    ClasspathApprovedKnowledgeSourceRepository(ResourceLoader resourceLoader, MarkdownKnowledgeDocumentParser parser) {
        this.resourceLoader = resourceLoader;
        this.parser = parser;
    }

    @Override
    public List<ApprovedKnowledgeDocument> findAll() {
        return SOURCES.stream().map(this::load).toList();
    }

    private ApprovedKnowledgeDocument load(String sourceName) {
        try {
            String markdown = resourceLoader
                    .getResource("classpath:knowledge/" + sourceName)
                    .getContentAsString(StandardCharsets.UTF_8);
            return parser.parse(sourceName, markdown);
        } catch (IOException exception) {
            throw new IllegalStateException("Approved knowledge source could not be read: " + sourceName, exception);
        }
    }
}
