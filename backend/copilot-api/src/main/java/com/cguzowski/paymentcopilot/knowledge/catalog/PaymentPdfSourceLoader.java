package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/** Reads only the accepted frozen library; no scenario or oracle inputs. */
final class PaymentPdfSourceLoader {
    static final String VERSION = "synten-payment-knowledge/v1";
    static final String FREEZE_HASH = "a1fe13333de61f51a9406827a577f880ca7d88b1bcd2e0a4def438f6b134b421";
    private final Path root;
    private final JsonMapper mapper;

    PaymentPdfSourceLoader(Path root, JsonMapper mapper) {
        this.root = root;
        this.mapper = mapper;
    }

    List<SynTenPdfSourceDocument> load() {
        try {
            byte[] freezeBytes = Files.readAllBytes(root.resolve("freeze-manifest.json"));
            require(hash(freezeBytes).equals(FREEZE_HASH), "Unaccepted payment PDF freeze hash.");
            var freeze = mapper.readTree(freezeBytes);
            require(VERSION.equals(freeze.get("corpusVersion").asString()), "Unaccepted payment PDF catalog version.");
            require(
                    hash(Files.readAllBytes(root.resolve("inventory.json")))
                            .equals(freeze.get("inventorySha256").asString()),
                    "Payment PDF inventory hash mismatch.");
            List<SynTenPdfSourceDocument> result = new ArrayList<>();
            for (var row : freeze.get("documents")) {
                byte[] source = read(row.get("source").asString(), "sources", "source");
                byte[] pdf = read(row.get("pdf").asString(), "pdfs", "PDF");
                require(hash(source).equals(row.get("sourceSha256").asString()), "Payment source hash mismatch.");
                require(hash(pdf).equals(row.get("pdfSha256").asString()), "Payment PDF hash mismatch.");
                Metadata m = metadata(source);
                require(
                        m.tenantId().equals(SynTenCorpusSourceRepository.SYNTEN_TENANT_ID.toString())
                                && m.key().equals(row.get("key").asString())
                                && m.documentId().equals(row.get("documentId").asString())
                                && m.version().equals(row.get("version").asString())
                                && "APPROVED".equals(m.status())
                                && !m.families().isEmpty(),
                        "Payment PDF metadata mismatch.");
                result.add(new SynTenPdfSourceDocument(
                        m.key(),
                        UUID.fromString(m.documentId()),
                        UUID.fromString(m.tenantId()),
                        KnowledgeDocumentType.valueOf(m.type()),
                        m.title(),
                        m.version(),
                        m.families().getFirst(),
                        String.join(", ", m.stages()),
                        KnowledgeApprovalStatus.APPROVED,
                        UUID.fromString("b3154ad3-6d18-4fb0-867a-55fe2c676b23"),
                        Instant.parse(m.effectiveDate() + "T00:00:00Z"),
                        Instant.parse(m.effectiveDate() + "T00:00:00Z"),
                        m.classification(),
                        null,
                        row.get("source").asString(),
                        m.filename(),
                        hash(source),
                        hash(pdf),
                        row.get("pageCount").asInt(),
                        source,
                        pdf,
                        m.related()));
            }
            require(result.size() == 16, "Unaccepted payment PDF document count.");
            return List.copyOf(result);
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Payment PDF library could not be read.", exception);
        }
    }

    private byte[] read(String name, String directory, String kind) throws java.io.IOException {
        Path relative = Path.of(name);
        Path path = root.resolve(relative).normalize();
        require(
                !relative.isAbsolute()
                        && relative.getNameCount() == 2
                        && relative.getName(0).toString().equals(directory)
                        && path.startsWith(root),
                "Invalid payment PDF artifact path.");
        require(Files.isRegularFile(path), "Payment " + kind + " artifact is missing: " + name);
        return Files.readAllBytes(path);
    }

    static Metadata metadata(byte[] source) {
        String text = new String(source, StandardCharsets.UTF_8).replace("\r\n", "\n");
        require(text.startsWith("---\n{"), "Invalid payment source metadata.");
        int end = text.indexOf("\n---\n", 4);
        require(end > 4, "Invalid payment source metadata.");
        return JsonMapper.builder().build().readValue(text.substring(4, end), Metadata.class);
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    record Metadata(
            String classification,
            String documentId,
            String effectiveDate,
            List<String> families,
            String filename,
            String key,
            String owner,
            List<String> related,
            List<String> risks,
            List<String> sources,
            List<String> stages,
            String status,
            String tenantId,
            String title,
            String type,
            String version) {}
}
