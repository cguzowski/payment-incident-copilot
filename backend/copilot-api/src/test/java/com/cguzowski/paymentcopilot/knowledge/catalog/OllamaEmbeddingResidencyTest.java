package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingProperties;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

class OllamaEmbeddingResidencyTest {
    @Test
    void configuredImmediateUnloadPreservesEmbeddingInputAndVectorOnTheWire() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment
                .getPropertySources()
                .addFirst(new MapPropertySource("local", Map.of("KNOWLEDGE_EMBEDDING_KEEP_ALIVE", "0s")));
        new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addLast);
        OllamaEmbeddingProperties properties = Binder.get(environment)
                .bind("spring.ai.ollama.embedding", Bindable.of(OllamaEmbeddingProperties.class))
                .get();
        assertThat(properties.getKeepAlive()).isEqualTo("0s");
        AtomicReference<String> request = new AtomicReference<>();
        JsonMapper json = JsonMapper.builder().build();
        float[] vector = new float[768];
        vector[0] = 1f;
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/embed", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = json.writeValueAsString(Map.of("model", "nomic-embed-text", "embeddings", List.of(vector)))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            var provider = OllamaEmbeddingModel.builder()
                    .ollamaApi(OllamaApi.builder()
                            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .build())
                    .options(properties.toOptions())
                    .build();
            KnowledgeEmbedding result = new SpringAiOllamaKnowledgeEmbeddingClient(
                            java.util.Optional.of(provider), properties.getKeepAlive(), properties.getModel())
                    .embed("exact synthetic query");
            assertThat(result.vector()).containsExactly(vector);
            assertThat(result.dimensions()).isEqualTo(768);
            var body = json.readTree(request.get());
            assertThat(body.path("model").asString()).isEqualTo("nomic-embed-text");
            assertThat(body.path("input").get(0).asString()).isEqualTo("exact synthetic query");
            assertThat(body.path("keep_alive").asString()).as(request.get()).isEqualTo("0s");
        } finally {
            server.stop(0);
        }
    }
}
