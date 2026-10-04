package com.cguzowski.paymentcopilot.report;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.util.MimeType;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.ObjectNode;

@Configuration(proxyBeanMethods = false)
class ReportOllamaTransportConfiguration {
    @Bean
    WebClientCustomizer reportContextPreservation() {
        // Spring AI 2.0's ChatRequest omits these top-level Ollama controls.
        // Register only for that request type; embeddings and other HTTP bodies
        // retain their existing codecs and serialization.
        JsonMapper mapper = JsonMapper.builder()
                .addModule(
                        new SimpleModule().addSerializer(OllamaApi.ChatRequest.class, new PreservedContextSerializer()))
                .build();
        JacksonJsonEncoder encoder = new JacksonJsonEncoder(mapper) {
            @Override
            public boolean canEncode(ResolvableType type, MimeType mimeType) {
                return type.toClass() == OllamaApi.ChatRequest.class && super.canEncode(type, mimeType);
            }
        };
        return builder -> builder.codecs(codecs -> codecs.customCodecs().register(encoder));
    }

    private static final class PreservedContextSerializer extends ValueSerializer<OllamaApi.ChatRequest> {
        private final JsonMapper original = JsonMapper.builder().build();

        @Override
        public void serialize(OllamaApi.ChatRequest value, JsonGenerator generator, SerializationContext context) {
            ObjectNode request = original.valueToTree(value);
            request.put("truncate", ReportGenerationSettings.MODEL_SETTINGS.truncate());
            request.put("shift", ReportGenerationSettings.MODEL_SETTINGS.shift());
            generator.writeTree(request);
        }
    }
}
