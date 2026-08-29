package com.cguzowski.paymentcopilot.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.knowledge.embedding-smoke-test", name = "enabled", havingValue = "true")
class KnowledgeEmbeddingSmokeTestCommand implements ApplicationRunner {

    static final String SMOKE_INPUT = "Synthetic Bedrock embedding contract smoke test.";
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeEmbeddingSmokeTestCommand.class);

    private final KnowledgeEmbeddingClient embeddingClient;

    KnowledgeEmbeddingSmokeTestCommand(KnowledgeEmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            KnowledgeEmbedding embedding = embeddingClient.embed(SMOKE_INPUT);
            if (!SpringAiTitanKnowledgeEmbeddingClient.MODEL_ID.equals(embedding.modelId())
                    || embedding.dimensions() != SpringAiTitanKnowledgeEmbeddingClient.DIMENSIONS
                    || !embedding.normalized()
                    || embedding.vector().length != SpringAiTitanKnowledgeEmbeddingClient.DIMENSIONS) {
                throw new IllegalStateException("Bedrock embedding smoke test failed its model contract.");
            }
            LOGGER.info(
                    "Bedrock embedding smoke test passed: modelId={}, dimensions={}, normalized={}",
                    embedding.modelId(),
                    embedding.dimensions(),
                    embedding.normalized());
        } catch (KnowledgeEmbeddingUnavailableException
                | KnowledgeEmbeddingTimedOutException
                | KnowledgeEmbeddingMalformedException exception) {
            LOGGER.error("Bedrock embedding smoke test failed safely: {}", exception.getMessage());
            throw exception;
        }
    }
}
