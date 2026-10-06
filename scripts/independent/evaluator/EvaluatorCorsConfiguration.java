package com.cguzowski.syntheticincidentgenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class EvaluatorCorsConfiguration implements WebMvcConfigurer {
    private final String[] origins;

    EvaluatorCorsConfiguration(@Value("${evaluation.browser-origins}") String[] origins) {
        this.origins = origins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/generations/*/answer-key")
                .allowedOrigins(origins)
                .allowedMethods("POST")
                .allowedHeaders("X-Synthetic-Operator-Id", "Content-Type");
        registry.addMapping("/api/generations/*/comparison")
                .allowedOrigins(origins)
                .allowedMethods("POST")
                .allowedHeaders("X-Synthetic-Operator-Id", "Content-Type");
    }
}
