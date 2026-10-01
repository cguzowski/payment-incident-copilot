package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
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
        return Stream.concat(
                        SOURCES.stream().map(name -> "knowledge/" + name),
                        List.of(
                                        "authorization-timeout",
                                        "capture-failure",
                                        "refund-failure",
                                        "settlement-delay",
                                        "webhook-delivery",
                                        "reconciliation-mismatch")
                                .stream()
                                .flatMap(name -> Stream.of(
                                        "knowledge/multi-incidents/v1/" + name + "-runbook.md",
                                        "knowledge/multi-incidents/v1/" + name + "-policy.md")))
                .map(this::load)
                .toList();
    }

    private ApprovedKnowledgeDocument load(String sourceName) {
        try {
            String markdown =
                    resourceLoader.getResource("classpath:" + sourceName).getContentAsString(StandardCharsets.UTF_8);
            return parser.parse(
                    sourceName.startsWith("knowledge/multi-incidents/")
                            ? sourceName
                            : sourceName.substring("knowledge/".length()),
                    markdown);
        } catch (IOException exception) {
            throw new IllegalStateException("Approved knowledge source could not be read: " + sourceName, exception);
        }
    }
}
