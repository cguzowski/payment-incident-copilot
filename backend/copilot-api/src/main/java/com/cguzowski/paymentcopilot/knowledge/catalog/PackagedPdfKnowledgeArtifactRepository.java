package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Repository;

@Repository
class PackagedPdfKnowledgeArtifactRepository implements PdfKnowledgeArtifactRepository {

    private static final String RESOURCE_PATTERN = "classpath*:knowledge/pdf-artifacts/**/*.pdf";
    private final Map<String, PdfKnowledgeArtifact> artifacts;

    PackagedPdfKnowledgeArtifactRepository(ResourcePatternResolver resources) {
        this.artifacts = load(resources);
    }

    @Override
    public Optional<PdfKnowledgeArtifact> findBySha256(String sha256) {
        return Optional.ofNullable(artifacts.get(sha256));
    }

    private static Map<String, PdfKnowledgeArtifact> load(ResourcePatternResolver resources) {
        Map<String, PdfKnowledgeArtifact> loaded = new LinkedHashMap<>();
        try {
            for (Resource resource : resources.getResources(RESOURCE_PATTERN)) {
                byte[] content = resource.getContentAsByteArray();
                String hash = sha256(content);
                loaded.putIfAbsent(hash, new PdfKnowledgeArtifact(hash, resource.getFilename(), content));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Packaged PDF artifacts could not be read.", exception);
        }
        return Map.copyOf(loaded);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
