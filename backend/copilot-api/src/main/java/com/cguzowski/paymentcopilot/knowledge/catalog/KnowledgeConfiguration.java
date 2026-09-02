package com.cguzowski.paymentcopilot.knowledge.catalog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KnowledgeConfiguration {

    @Bean
    TokenEstimator knowledgeTokenEstimator() {
        return new ApproximateTokenEstimator();
    }

    @Bean
    MarkdownKnowledgeChunker markdownKnowledgeChunker(TokenEstimator knowledgeTokenEstimator) {
        return new MarkdownKnowledgeChunker(knowledgeTokenEstimator, 400, 600, 50);
    }

    @Bean
    PdfKnowledgeChunker pdfKnowledgeChunker(TokenEstimator knowledgeTokenEstimator) {
        return new PdfKnowledgeChunker(knowledgeTokenEstimator, 400, 600, 50, 80);
    }
}
