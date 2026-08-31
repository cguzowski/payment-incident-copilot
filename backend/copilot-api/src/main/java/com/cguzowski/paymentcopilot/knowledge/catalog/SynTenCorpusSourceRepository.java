package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
class SynTenCorpusSourceRepository {

    static final UUID SYNTEN_TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    static final String CORPUS_VERSION = "synten-auth-knowledge/v1";
    private static final String INCIDENT_FAMILY = "AUTHORIZATION_DECLINE_RATE_SPIKE";
    private static final int DOCUMENT_COUNT = 30;
    private static final int MAXIMUM_PAGES = 15;

    private final Path corpusRoot;
    private final JsonMapper jsonMapper;

    @Autowired
    SynTenCorpusSourceRepository(
            @Value("${app.knowledge.pdf-catalog.corpus-root:}") String configuredRoot, JsonMapper jsonMapper) {
        this(configuredRoot.isBlank() ? null : Path.of(configuredRoot), jsonMapper);
    }

    SynTenCorpusSourceRepository(Path corpusRoot, JsonMapper jsonMapper) {
        this.corpusRoot =
                corpusRoot == null ? null : corpusRoot.toAbsolutePath().normalize();
        this.jsonMapper = jsonMapper;
    }

    List<SynTenPdfSourceDocument> findAll() {
        if (corpusRoot == null) {
            throw new IllegalStateException("SynTen PDF catalog corpus root is required.");
        }
        Manifest manifest = readManifest();
        validateManifest(manifest);
        validateExactArtifacts(manifest);

        List<SynTenPdfSourceDocument> documents = new ArrayList<>();
        Set<DocumentVersionKey> versions = new HashSet<>();
        for (ManifestDocument entry : manifest.documents()) {
            SynTenPdfSourceDocument document = load(entry);
            DocumentVersionKey version =
                    new DocumentVersionKey(document.tenantId(), document.documentId(), document.version());
            if (!versions.add(version)) {
                throw invalid("SynTen manifest contains a duplicate tenant/document/version: " + entry.key());
            }
            documents.add(document);
        }
        return List.copyOf(documents);
    }

    private Manifest readManifest() {
        Path manifestPath = corpusRoot.resolve("validation-manifest.json");
        try {
            return jsonMapper.readValue(Files.readString(manifestPath, StandardCharsets.UTF_8), Manifest.class);
        } catch (IOException | JacksonException exception) {
            throw invalid("SynTen validation manifest could not be read.", exception);
        }
    }

    private static void validateManifest(Manifest manifest) {
        if (!CORPUS_VERSION.equals(manifest.corpusVersion())
                || manifest.documentCount() != DOCUMENT_COUNT
                || manifest.documents() == null
                || manifest.documents().size() != DOCUMENT_COUNT) {
            throw invalid("SynTen validation manifest contract is invalid.");
        }
    }

    private void validateExactArtifacts(Manifest manifest) {
        Set<String> expectedSources = new HashSet<>();
        Set<String> expectedPdfs = new HashSet<>();
        for (ManifestDocument entry : manifest.documents()) {
            expectedSources.add(fileName(validateRelativePath(entry.source(), "sources", entry.key())));
            expectedPdfs.add(fileName(validateRelativePath(entry.pdf(), "pdfs", entry.key())));
        }
        if (!expectedSources.equals(fileNames(corpusRoot.resolve("sources")))
                || !expectedPdfs.equals(fileNames(corpusRoot.resolve("pdfs")))) {
            throw invalid("SynTen corpus artifacts do not exactly match the manifest.");
        }
    }

    private SynTenPdfSourceDocument load(ManifestDocument entry) {
        String key = bounded(entry.key(), "document key", 20);
        Path sourcePath = validateRelativePath(entry.source(), "sources", key);
        Path pdfPath = validateRelativePath(entry.pdf(), "pdfs", key);
        byte[] sourceBytes = readArtifact(sourcePath, "maintained source", key);
        byte[] pdfBytes = readArtifact(pdfPath, "PDF", key);
        if (!sha256(sourceBytes).equals(entry.sourceSha256())) {
            throw invalid("SynTen maintained source hash does not match the manifest: " + key);
        }
        if (!sha256(pdfBytes).equals(entry.pdfSha256())) {
            throw invalid("SynTen PDF hash does not match the manifest: " + key);
        }
        if (entry.pageCount() < 1 || entry.pageCount() > MAXIMUM_PAGES) {
            throw invalid("SynTen manifest page count is outside 1-15: " + key);
        }

        Map<String, String> metadata = parseFrontMatter(sourceBytes, key);
        requireMatch(metadata, "documentKey", key, key);
        requireMatch(metadata, "documentId", entry.documentId(), key);
        requireMatch(metadata, "version", entry.version(), key);
        requireMatch(metadata, "type", entry.type(), key);
        requireMatch(metadata, "approvalStatus", entry.approvalStatus(), key);
        requireMatch(metadata, "incidentFamily", entry.incidentFamily(), key);
        requireMatch(metadata, "replacement", entry.replacement() == null ? "None" : entry.replacement(), key);

        UUID tenantId = uuid(metadata, "tenantId", key);
        if (!SYNTEN_TENANT_ID.equals(tenantId)) {
            throw invalid("SynTen maintained source has the wrong tenant: " + key);
        }
        if (!INCIDENT_FAMILY.equals(entry.incidentFamily())) {
            throw invalid("SynTen manifest incident family is unsupported: " + key);
        }

        return new SynTenPdfSourceDocument(
                key,
                uuid(entry.documentId(), "document ID", key),
                tenantId,
                enumValue(KnowledgeDocumentType.class, entry.type(), "type", key),
                bounded(metadata.get("title"), "title", 160),
                bounded(entry.version(), "version", 40),
                bounded(entry.incidentFamily(), "incident family", 80),
                bounded(metadata.get("appliesTo"), "applies to", 120),
                approvalStatus(entry.approvalStatus(), key),
                uuid(metadata, "approvedBy", key),
                instant(metadata, "approvedAt", key),
                instant(metadata, "effectiveAt", key),
                bounded(metadata.get("classification"), "classification", 80),
                entry.replacement(),
                sourcePath.getFileName().toString(),
                pdfPath.getFileName().toString(),
                entry.sourceSha256(),
                entry.pdfSha256(),
                entry.pageCount(),
                sourceBytes,
                pdfBytes);
    }

    private Path validateRelativePath(String relativeValue, String expectedDirectory, String key) {
        if (relativeValue == null || relativeValue.isBlank()) {
            throw invalid("SynTen manifest artifact path is invalid: " + key);
        }
        Path relative = Path.of(relativeValue.replace('/', java.io.File.separatorChar));
        if (relative.isAbsolute()
                || relative.getNameCount() != 2
                || !relative.getName(0).toString().equals(expectedDirectory)) {
            throw invalid("SynTen manifest artifact path is invalid: " + key);
        }
        Path resolved = corpusRoot.resolve(relative).normalize();
        if (!resolved.startsWith(corpusRoot)) {
            throw invalid("SynTen manifest artifact path is invalid: " + key);
        }
        return resolved;
    }

    private static byte[] readArtifact(Path path, String kind, String key) {
        try {
            if (!Files.isRegularFile(path)) {
                throw invalid("SynTen " + kind + " is missing: " + key);
            }
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw invalid("SynTen " + kind + " could not be read: " + key, exception);
        }
    }

    private static Set<String> fileNames(Path directory) {
        try (var paths = Files.list(directory)) {
            Set<String> names = new HashSet<>();
            paths.filter(Files::isRegularFile)
                    .forEach(path -> names.add(path.getFileName().toString()));
            return names;
        } catch (IOException exception) {
            throw invalid("SynTen corpus artifact directory could not be read.", exception);
        }
    }

    private static Map<String, String> parseFrontMatter(byte[] sourceBytes, String key) {
        String source = new String(sourceBytes, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        if (!source.startsWith("---\n")) {
            throw invalid("SynTen maintained source front matter is invalid: " + key);
        }
        int end = source.indexOf("\n---\n", 4);
        if (end < 0) {
            throw invalid("SynTen maintained source front matter is invalid: " + key);
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String line : source.substring(4, end).split("\n", -1)) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw invalid("SynTen maintained source front matter is invalid: " + key);
            }
            String field = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (field.isBlank() || value.isBlank() || metadata.putIfAbsent(field, value) != null) {
                throw invalid("SynTen maintained source front matter is invalid: " + key);
            }
        }
        return metadata;
    }

    private static void requireMatch(Map<String, String> metadata, String field, String expected, String key) {
        if (!expected.equals(metadata.get(field))) {
            throw invalid("SynTen manifest/source metadata disagree: " + key + " (" + field + ")");
        }
    }

    private static UUID uuid(Map<String, String> metadata, String field, String key) {
        return uuid(metadata.get(field), field, key);
    }

    private static UUID uuid(String value, String field, String key) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw invalid("SynTen " + field + " is invalid: " + key, exception);
        }
    }

    private static Instant instant(Map<String, String> metadata, String field, String key) {
        try {
            return Instant.parse(metadata.get(field));
        } catch (DateTimeException | NullPointerException exception) {
            throw invalid("SynTen " + field + " is invalid: " + key, exception);
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field, String key) {
        try {
            return Enum.valueOf(type, value);
        } catch (RuntimeException exception) {
            throw invalid("SynTen " + field + " is invalid: " + key, exception);
        }
    }

    private static KnowledgeApprovalStatus approvalStatus(String value, String key) {
        KnowledgeApprovalStatus status = enumValue(KnowledgeApprovalStatus.class, value, "approval status", key);
        if (status != KnowledgeApprovalStatus.APPROVED && status != KnowledgeApprovalStatus.SUPERSEDED) {
            throw invalid("SynTen approval status is invalid: " + key);
        }
        return status;
    }

    private static String bounded(String value, String field, int maximumLength) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("SynTen " + field + " is invalid.");
        }
        return value;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static String fileName(Path path) {
        return path.getFileName().toString();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }

    private record DocumentVersionKey(UUID tenantId, UUID documentId, String version) {}

    private record Manifest(
            String corpusVersion,
            String authoringStandardVersion,
            String generatorVersion,
            String validatedAt,
            int documentCount,
            int totalPages,
            int minimumPages,
            int maximumPages,
            int medianPages,
            List<ManifestDocument> documents) {}

    private record ManifestDocument(
            String key,
            String documentId,
            String version,
            String type,
            String approvalStatus,
            String incidentFamily,
            String source,
            String pdf,
            String sourceSha256,
            String pdfSha256,
            int pageCount,
            int extractedCharacters,
            List<String> requiredErrorCodes,
            String replacement,
            Map<String, String> checks) {}
}
