package com.cguzowski.paymentcopilot.knowledge.retrieval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
class SynTenRetrievalEvaluationContractRepository {

    static final String EVALUATION_VERSION = "synten-retrieval-eval/v1";
    static final String CORPUS_VERSION = "synten-auth-knowledge/v1";
    static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    static final String INCIDENT_FAMILY = "AUTHORIZATION_DECLINE_RATE_SPIKE";
    static final Set<String> SUPERSEDED_KEYS = Set.of("RB-022", "PL-007", "PL-008");

    private static final Pattern CASE_ROW = Pattern.compile("^\\|\\s*(KQ-\\d{3})\\s*\\|(.*)\\|$");
    private static final Pattern SCENARIO_ID = Pattern.compile("S\\d{3}");
    private static final Set<String> MANIFEST_CHECKS = Set.of(
            "inventoryMetadata",
            "pageRange1To15",
            "unencrypted",
            "textExtraction",
            "pageNumbering",
            "requiredVocabulary",
            "supersession",
            "sensitivePatterns");

    private final Path casesPath;
    private final Path scenarioCatalogPath;
    private final Path corpusRoot;
    private final JsonMapper jsonMapper;

    @Autowired
    SynTenRetrievalEvaluationContractRepository(
            @Value("${app.knowledge.retrieval-evaluation.cases-path:}") String configuredCasesPath,
            @Value("${app.knowledge.retrieval-evaluation.scenario-catalog-path:}") String configuredScenarioCatalogPath,
            @Value("${app.knowledge.retrieval-evaluation.corpus-root:}") String configuredCorpusRoot,
            JsonMapper jsonMapper) {
        this(path(configuredCasesPath), path(configuredScenarioCatalogPath), path(configuredCorpusRoot), jsonMapper);
    }

    SynTenRetrievalEvaluationContractRepository(
            Path casesPath, Path scenarioCatalogPath, Path corpusRoot, JsonMapper jsonMapper) {
        this.casesPath = normalized(casesPath);
        this.scenarioCatalogPath = normalized(scenarioCatalogPath);
        this.corpusRoot = normalized(corpusRoot);
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    SynTenRetrievalEvaluationContract load(Instant evaluatedAt) {
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
        requireConfigured();
        ParsedCases parsedCases = parseCases(read(casesPath, "retrieval cases"));
        Map<String, EvaluationScenario> scenarios = loadScenarios();
        Map<String, EvaluationDocument> documents = loadDocuments(evaluatedAt);
        validateSources(parsedCases.cases(), scenarios, documents);
        return new SynTenRetrievalEvaluationContract(
                parsedCases.evaluationVersion(),
                parsedCases.corpusVersion(),
                evaluatedAt,
                parsedCases.cases(),
                scenarios,
                documents);
    }

    private void requireConfigured() {
        if (casesPath == null || scenarioCatalogPath == null || corpusRoot == null) {
            throw invalid("SynTen retrieval evaluation paths are required.");
        }
    }

    private static ParsedCases parseCases(String markdown) {
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        requireLine(normalized, "Status: Approved evaluation design");
        requireLine(normalized, "Evaluation version: `" + EVALUATION_VERSION + "`");
        requireLine(normalized, "Corpus version: `" + CORPUS_VERSION + "`");

        List<RetrievalEvaluationCase> cases = new ArrayList<>();
        for (String line : normalized.split("\n", -1)) {
            Matcher matcher = CASE_ROW.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String[] cells = matcher.group(2).split("\\|", -1);
            if (cells.length != 6) {
                throw invalid("SynTen retrieval evaluation case table is malformed.");
            }
            String caseId = matcher.group(1);
            String scenariosCell = cell(cells[0], "scenarios", caseId);
            List<String> scenarioIds;
            if (caseId.equals("KQ-023")) {
                if (!scenariosCell.equals("Synthetic exclusion probe")) {
                    throw invalid("KQ-023 must remain the synthetic exclusion probe.");
                }
                scenarioIds = List.of();
            } else {
                scenarioIds = parseScenarioIds(scenariosCell, caseId);
            }
            cases.add(new RetrievalEvaluationCase(
                    caseId,
                    scenarioIds,
                    cell(cells[1], "query signals", caseId),
                    documentKey(cells[2], "primary runbook", caseId, false),
                    documentKey(cells[3], "supporting policy", caseId, false),
                    documentKey(cells[4], "weak approved match", caseId, true),
                    cell(cells[5], "expected report posture", caseId)));
        }
        if (!cases.stream().map(RetrievalEvaluationCase::caseId).toList().equals(List.copyOf(expectedCaseIds()))) {
            throw invalid("SynTen retrieval evaluation must contain exactly KQ-001 through KQ-023 in order.");
        }
        return new ParsedCases(EVALUATION_VERSION, CORPUS_VERSION, cases);
    }

    private Map<String, EvaluationScenario> loadScenarios() {
        ScenarioCatalog catalog;
        try {
            catalog = jsonMapper.readValue(read(scenarioCatalogPath, "scenario catalog"), ScenarioCatalog.class);
        } catch (JacksonException exception) {
            throw invalid("SynTen scenario catalog is malformed.", exception);
        }
        if (catalog == null || catalog.scenarios() == null) {
            throw invalid("SynTen scenario catalog is malformed.");
        }
        Map<String, EvaluationScenario> scenarios = new LinkedHashMap<>();
        for (EvaluationScenario scenario : catalog.scenarios()) {
            validateScenario(scenario);
            if (scenarios.putIfAbsent(scenario.code(), scenario) != null) {
                throw invalid("SynTen scenario catalog contains a duplicate scenario: " + scenario.code() + ".");
            }
        }
        if (!scenarios.keySet().equals(expectedScenarioIds())) {
            throw invalid("SynTen scenario coverage differs from the exact 36 reviewed scenarios.");
        }
        return scenarios;
    }

    private Map<String, EvaluationDocument> loadDocuments(Instant evaluatedAt) {
        Manifest manifest;
        try {
            manifest = jsonMapper.readValue(
                    read(corpusRoot.resolve("validation-manifest.json"), "validation manifest"), Manifest.class);
        } catch (JacksonException exception) {
            throw invalid("SynTen validation manifest is malformed.", exception);
        }
        if (manifest == null
                || !CORPUS_VERSION.equals(manifest.corpusVersion())
                || manifest.documentCount() != 30
                || manifest.documents() == null
                || manifest.documents().size() != 30) {
            throw invalid("SynTen validation manifest contract is invalid.");
        }

        Map<String, EvaluationDocument> documents = new LinkedHashMap<>();
        for (ManifestDocument entry : manifest.documents()) {
            EvaluationDocument document = loadDocument(entry, evaluatedAt);
            if (documents.putIfAbsent(document.key(), document) != null) {
                throw invalid("SynTen validation manifest contains a duplicate document key: " + document.key() + ".");
            }
        }
        if (!documents.keySet().equals(expectedDocumentKeys())) {
            throw invalid("SynTen validation manifest has drifted document membership.");
        }
        Set<String> superseded = new HashSet<>();
        documents.values().stream()
                .filter(document -> document.approvalStatus().equals("SUPERSEDED"))
                .forEach(document -> superseded.add(document.key()));
        if (!superseded.equals(SUPERSEDED_KEYS)) {
            throw invalid("SynTen validation manifest must contain exactly the three reviewed superseded keys.");
        }
        return documents;
    }

    private EvaluationDocument loadDocument(ManifestDocument entry, Instant evaluatedAt) {
        validateManifestEntry(entry);
        Path sourcePath = safeSourcePath(entry.source(), entry.key());
        Map<String, String> metadata = frontMatter(read(sourcePath, "maintained source " + entry.key()), entry.key());
        requireAgreement(metadata, "documentKey", entry.key());
        requireAgreement(metadata, "documentId", entry.documentId());
        requireAgreement(metadata, "version", entry.version());
        requireAgreement(metadata, "type", entry.type());
        requireAgreement(metadata, "approvalStatus", entry.approvalStatus());
        requireAgreement(metadata, "incidentFamily", entry.incidentFamily());
        requireAgreement(metadata, "tenantId", TENANT_ID.toString());
        Instant approvedAt = instant(metadata.get("approvedAt"), "approvedAt", entry.key());
        Instant effectiveAt = instant(metadata.get("effectiveAt"), "effectiveAt", entry.key());
        if (approvedAt.isAfter(evaluatedAt) || effectiveAt.isAfter(evaluatedAt)) {
            throw invalid("SynTen evaluation document is not eligible at the evaluation instant: " + entry.key() + ".");
        }
        return new EvaluationDocument(
                entry.key(),
                uuid(entry.documentId(), entry.key()),
                entry.version(),
                entry.type(),
                entry.approvalStatus(),
                entry.incidentFamily(),
                entry.source(),
                entry.pdf(),
                entry.sourceSha256(),
                entry.pdfSha256(),
                entry.pageCount(),
                entry.replacement(),
                approvedAt,
                effectiveAt);
    }

    private static void validateSources(
            List<RetrievalEvaluationCase> cases,
            Map<String, EvaluationScenario> scenarios,
            Map<String, EvaluationDocument> documents) {
        Set<String> covered = new HashSet<>();
        for (RetrievalEvaluationCase evaluationCase : cases) {
            for (String scenarioId : evaluationCase.scenarioIds()) {
                if (!scenarios.containsKey(scenarioId) || !covered.add(scenarioId)) {
                    throw invalid("SynTen retrieval evaluation scenario coverage is missing or duplicated.");
                }
            }
            requireEligibleDocument(documents, evaluationCase.primaryRunbookKey(), "RUNBOOK", "primary runbook");
            requireEligibleDocument(documents, evaluationCase.supportingPolicyKey(), "POLICY", "supporting policy");
            if (evaluationCase.weakApprovedMatchKey() != null) {
                requireEligibleDocument(
                        documents, evaluationCase.weakApprovedMatchKey(), "RUNBOOK", "weak approved match");
            }
        }
        if (!covered.equals(scenarios.keySet())) {
            throw invalid(
                    "SynTen retrieval evaluation scenario coverage differs from the exact 36 reviewed scenarios.");
        }
    }

    private static void requireEligibleDocument(
            Map<String, EvaluationDocument> documents, String key, String type, String role) {
        EvaluationDocument document = documents.get(key);
        if (document == null) {
            throw invalid("SynTen retrieval evaluation references an unknown " + role + ": " + key + ".");
        }
        if (!type.equals(document.type()) || !"APPROVED".equals(document.approvalStatus())) {
            throw invalid("SynTen retrieval evaluation " + role + " is not eligible: " + key + ".");
        }
    }

    private static void validateManifestEntry(ManifestDocument entry) {
        if (entry == null
                || !expectedDocumentKeys().contains(entry.key())
                || !Set.of("RUNBOOK", "POLICY").contains(entry.type())
                || !Set.of("APPROVED", "SUPERSEDED").contains(entry.approvalStatus())
                || !INCIDENT_FAMILY.equals(entry.incidentFamily())
                || entry.pageCount() < 1
                || entry.pageCount() > 15
                || !sha256(entry.sourceSha256())
                || !sha256(entry.pdfSha256())
                || entry.checks() == null
                || !entry.checks().keySet().equals(MANIFEST_CHECKS)
                || entry.checks().values().stream().anyMatch(value -> !"PASS".equals(value))) {
            throw invalid("SynTen validation manifest document contract is invalid.");
        }
    }

    private static void validateScenario(EvaluationScenario scenario) {
        if (scenario == null
                || scenario.code() == null
                || !SCENARIO_ID.matcher(scenario.code()).matches()
                || blank(scenario.rarity())
                || blank(scenario.severity())
                || blank(scenario.title())
                || blank(scenario.description())
                || scenario.evidence() == null
                || blank(scenario.evidence().availability())
                || scenario.evidence().errors() == null
                || scenario.truth() == null
                || blank(scenario.truth().expectedDisposition())
                || blank(scenario.truth().expectedConfidence())) {
            throw invalid("SynTen scenario catalog contains a malformed scenario.");
        }
    }

    private Path safeSourcePath(String relativeValue, String key) {
        if (relativeValue == null || relativeValue.isBlank()) {
            throw invalid("SynTen validation manifest source path is invalid: " + key + ".");
        }
        Path relative = Path.of(relativeValue.replace('/', java.io.File.separatorChar));
        if (relative.isAbsolute()
                || relative.getNameCount() != 2
                || !relative.getName(0).toString().equals("sources")) {
            throw invalid("SynTen validation manifest source path is invalid: " + key + ".");
        }
        Path resolved = corpusRoot.resolve(relative).normalize();
        if (!resolved.startsWith(corpusRoot)) {
            throw invalid("SynTen validation manifest source path is invalid: " + key + ".");
        }
        return resolved;
    }

    private static Map<String, String> frontMatter(String source, String key) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.startsWith("---\n")) {
            throw invalid("SynTen maintained source front matter is invalid: " + key + ".");
        }
        int end = normalized.indexOf("\n---\n", 4);
        if (end < 0) {
            throw invalid("SynTen maintained source front matter is invalid: " + key + ".");
        }
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String line : normalized.substring(4, end).split("\n", -1)) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw invalid("SynTen maintained source front matter is invalid: " + key + ".");
            }
            String field = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (field.isBlank() || value.isBlank() || metadata.putIfAbsent(field, value) != null) {
                throw invalid("SynTen maintained source front matter is invalid: " + key + ".");
            }
        }
        return metadata;
    }

    private static void requireAgreement(Map<String, String> metadata, String field, String expected) {
        if (!Objects.equals(metadata.get(field), expected)) {
            throw invalid("SynTen validation manifest and maintained source disagree on " + field + ".");
        }
    }

    private static List<String> parseScenarioIds(String value, String caseId) {
        List<String> values = new ArrayList<>();
        for (String candidate : value.split(",", -1)) {
            String scenarioId = candidate.trim();
            if (!SCENARIO_ID.matcher(scenarioId).matches()) {
                throw invalid("SynTen retrieval evaluation has an invalid scenario in " + caseId + ".");
            }
            values.add(scenarioId);
        }
        if (values.isEmpty() || new HashSet<>(values).size() != values.size()) {
            throw invalid("SynTen retrieval evaluation has duplicate scenario coverage in " + caseId + ".");
        }
        return values;
    }

    private static String documentKey(String value, String role, String caseId, boolean nullable) {
        String key = cell(value, role, caseId);
        if (nullable && key.equals("none")) {
            return null;
        }
        if (!key.matches("(?:RB|PL)-\\d{3}")) {
            throw invalid("SynTen retrieval evaluation has an invalid " + role + " in " + caseId + ".");
        }
        return key;
    }

    private static String cell(String value, String field, String caseId) {
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            throw invalid("SynTen retrieval evaluation has an empty " + field + " in " + caseId + ".");
        }
        return trimmed;
    }

    private static void requireLine(String source, String exactLine) {
        if (!List.of(source.split("\n", -1)).contains(exactLine)) {
            throw invalid("SynTen retrieval evaluation header is invalid.");
        }
    }

    private static Instant instant(String value, String field, String key) {
        try {
            return Instant.parse(value);
        } catch (DateTimeException | NullPointerException exception) {
            throw invalid("SynTen " + field + " is invalid: " + key + ".", exception);
        }
    }

    private static UUID uuid(String value, String key) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw invalid("SynTen document ID is invalid: " + key + ".", exception);
        }
    }

    private static String read(Path path, String description) {
        try {
            if (path == null || !Files.isRegularFile(path)) {
                throw invalid("SynTen " + description + " is missing.");
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw invalid("SynTen " + description + " could not be read.", exception);
        }
    }

    private static boolean sha256(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static Set<String> expectedCaseIds() {
        Set<String> values = new LinkedHashSet<>();
        IntStream.rangeClosed(1, 23).forEach(number -> values.add("KQ-%03d".formatted(number)));
        return values;
    }

    private static Set<String> expectedScenarioIds() {
        Set<String> values = new LinkedHashSet<>();
        IntStream.rangeClosed(1, 14).forEach(number -> values.add("S%03d".formatted(number)));
        IntStream.rangeClosed(101, 111).forEach(number -> values.add("S%03d".formatted(number)));
        IntStream.rangeClosed(201, 211).forEach(number -> values.add("S%03d".formatted(number)));
        return values;
    }

    private static Set<String> expectedDocumentKeys() {
        Set<String> values = new LinkedHashSet<>();
        IntStream.rangeClosed(1, 22).forEach(number -> values.add("RB-%03d".formatted(number)));
        IntStream.rangeClosed(1, 8).forEach(number -> values.add("PL-%03d".formatted(number)));
        return values;
    }

    private static Path path(String configured) {
        return configured == null || configured.isBlank() ? null : Path.of(configured);
    }

    private static Path normalized(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }

    private record ParsedCases(String evaluationVersion, String corpusVersion, List<RetrievalEvaluationCase> cases) {}

    private record ScenarioCatalog(List<EvaluationScenario> scenarios) {}

    private record Manifest(
            String corpusVersion,
            String authoringStandardVersion,
            String generatorVersion,
            String validatedAt,
            int documentCount,
            int totalPages,
            int minimumPages,
            int maximumPages,
            double medianPages,
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
