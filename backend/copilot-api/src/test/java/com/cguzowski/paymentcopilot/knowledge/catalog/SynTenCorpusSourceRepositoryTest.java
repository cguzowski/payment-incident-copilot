package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class SynTenCorpusSourceRepositoryTest {

    private static final Path CORPUS_ROOT =
            Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize();

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsTheExactThirtyManifestVersionsInOrder() {
        SynTenCorpusSourceRepository repository = repository(CORPUS_ROOT);

        List<SynTenPdfSourceDocument> documents = repository.findAll();

        assertThat(documents).hasSize(30);
        assertThat(documents)
                .extracting(SynTenPdfSourceDocument::documentKey)
                .startsWith("RB-001", "RB-002")
                .endsWith("PL-007", "PL-008")
                .doesNotHaveDuplicates();
        assertThat(documents)
                .extracting(SynTenPdfSourceDocument::tenantId)
                .containsOnly(SynTenCorpusSourceRepository.SYNTEN_TENANT_ID);
        assertThat(documents)
                .filteredOn(document -> document.approvalStatus() == KnowledgeApprovalStatus.APPROVED)
                .hasSize(27);
        assertThat(documents)
                .filteredOn(document -> document.approvalStatus() == KnowledgeApprovalStatus.SUPERSEDED)
                .hasSize(3);
        assertThat(documents).allSatisfy(document -> {
            assertThat(document.sourceSha256()).matches("[0-9a-f]{64}");
            assertThat(document.pdfSha256()).matches("[0-9a-f]{64}");
            assertThat(document.sourceBytes()).isNotEmpty();
            assertThat(document.pdfBytes()).isNotEmpty();
            assertThat(document.sourceName()).endsWith(".md").doesNotContain("\\", "/");
            assertThat(document.pdfName()).endsWith(".pdf").doesNotContain("\\", "/");
            assertThat(document.manifestPageCount()).isBetween(1, 15);
        });
    }

    @Test
    void rejectsChangedSourceBeforePdfParsing() throws IOException {
        Path copiedCorpus = copyCorpus();
        Path source = copiedCorpus.resolve("sources/rb-001-authorization-decline-incident-triage-v2.0.0.md");
        Files.writeString(source, Files.readString(source) + "\nchanged under the same version\n");

        assertThatThrownBy(() -> repository(copiedCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen maintained source hash does not match the manifest: RB-001");
    }

    @Test
    void rejectsChangedPdfBeforePdfParsing() throws IOException {
        Path copiedCorpus = copyCorpus();
        Path pdf = copiedCorpus.resolve("pdfs/rb-001-authorization-decline-incident-triage-v2.0.0.pdf");
        Files.write(pdf, new byte[] {0}, java.nio.file.StandardOpenOption.APPEND);

        assertThatThrownBy(() -> repository(copiedCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF hash does not match the manifest: RB-001");
    }

    @Test
    void rejectsMissingOrExtraArtifacts() throws IOException {
        Path missingCorpus = copyCorpusTo("missing");
        Files.delete(missingCorpus.resolve("pdfs/pl-008-legacy-ai-incident-automation-policy-v1.0.0.pdf"));
        assertThatThrownBy(() -> repository(missingCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen corpus artifacts do not exactly match the manifest.");

        Path extraCorpus = copyCorpusTo("extra");
        Files.copy(
                extraCorpus.resolve("pdfs/pl-001-payment-incident-response-governance-v2.0.0.pdf"),
                extraCorpus.resolve("pdfs/unlisted.pdf"));
        assertThatThrownBy(() -> repository(extraCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen corpus artifacts do not exactly match the manifest.");
    }

    @Test
    void rejectsManifestPathEscape() throws IOException {
        Path copiedCorpus = copyCorpus();
        replaceInFile(
                copiedCorpus.resolve("validation-manifest.json"),
                "sources/rb-001-authorization-decline-incident-triage-v2.0.0.md",
                "../rb-001.md");

        assertThatThrownBy(() -> repository(copiedCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen manifest artifact path is invalid: RB-001");
    }

    @Test
    void rejectsWrongTenantAndManifestSourceDisagreement() throws IOException {
        Path wrongTenantCorpus = copyCorpusTo("wrong-tenant");
        updateSourceAndManifestHash(
                wrongTenantCorpus,
                "sources/rb-001-authorization-decline-incident-triage-v2.0.0.md",
                "tenantId: 8b860d80-d17f-4e6b-8c48-af35f26a4d61",
                "tenantId: 076a18a3-d54f-486a-b3ec-189e1048fd28");
        assertThatThrownBy(() -> repository(wrongTenantCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen maintained source has the wrong tenant: RB-001");

        Path disagreementCorpus = copyCorpusTo("disagreement");
        updateSourceAndManifestHash(
                disagreementCorpus,
                "sources/rb-001-authorization-decline-incident-triage-v2.0.0.md",
                "type: RUNBOOK",
                "type: POLICY");
        assertThatThrownBy(() -> repository(disagreementCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen manifest/source metadata disagree: RB-001 (type)");
    }

    @Test
    void rejectsUnsupportedMetadataAndDuplicateDocumentVersion() throws IOException {
        Path unsupportedCorpus = copyCorpusTo("unsupported");
        replaceInFile(
                unsupportedCorpus.resolve("validation-manifest.json"), "\"type\": \"RUNBOOK\"", "\"type\": \"GUIDE\"");
        updateSourceAndManifestHash(
                unsupportedCorpus,
                "sources/rb-001-authorization-decline-incident-triage-v2.0.0.md",
                "type: RUNBOOK",
                "type: GUIDE");
        assertThatThrownBy(() -> repository(unsupportedCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen type is invalid: RB-001");

        Path draftCorpus = copyCorpusTo("draft");
        updateSourceAndManifestHash(
                draftCorpus,
                "sources/rb-001-authorization-decline-incident-triage-v2.0.0.md",
                "approvalStatus: APPROVED",
                "approvalStatus: DRAFT");
        Path draftManifest = draftCorpus.resolve("validation-manifest.json");
        Files.writeString(
                draftManifest,
                Files.readString(draftManifest)
                        .replaceFirst("\"approvalStatus\": \"APPROVED\"", "\"approvalStatus\": \"DRAFT\""));
        assertThatThrownBy(() -> repository(draftCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen approval status is invalid: RB-001");

        Path duplicateCorpus = copyCorpusTo("duplicate");
        updateSourceAndManifestHash(
                duplicateCorpus,
                "sources/pl-008-legacy-ai-incident-automation-policy-v1.0.0.md",
                "version: 1.0.0",
                "version: 2.0.0");
        String manifest = Files.readString(duplicateCorpus.resolve("validation-manifest.json"));
        int pl008 = manifest.indexOf("\"key\": \"PL-008\"");
        String before = manifest.substring(0, pl008);
        String entry = manifest.substring(pl008).replaceFirst("\"version\": \"1\\.0\\.0\"", "\"version\": \"2.0.0\"");
        Files.writeString(duplicateCorpus.resolve("validation-manifest.json"), before + entry);
        assertThatThrownBy(() -> repository(duplicateCorpus).findAll())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen manifest contains a duplicate tenant/document/version: PL-008");
    }

    private SynTenCorpusSourceRepository repository(Path root) {
        return new SynTenCorpusSourceRepository(root, JsonMapper.builder().build());
    }

    private Path copyCorpus() throws IOException {
        return copyCorpusTo("corpus");
    }

    private Path copyCorpusTo(String directoryName) throws IOException {
        Path copied = temporaryDirectory.resolve(directoryName);
        try (var paths = Files.walk(CORPUS_ROOT)) {
            for (Path source : paths.toList()) {
                Path target = copied.resolve(CORPUS_ROOT.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
        return copied;
    }

    private static void updateSourceAndManifestHash(
            Path corpus, String relativeSource, String oldValue, String newValue) throws IOException {
        Path source = corpus.resolve(relativeSource);
        byte[] original = Files.readAllBytes(source);
        String oldHash = sha256(original);
        Files.writeString(
                source, new String(original, java.nio.charset.StandardCharsets.UTF_8).replace(oldValue, newValue));
        replaceInFile(corpus.resolve("validation-manifest.json"), oldHash, sha256(Files.readAllBytes(source)));
    }

    private static void replaceInFile(Path path, String oldValue, String newValue) throws IOException {
        Files.writeString(path, Files.readString(path).replace(oldValue, newValue));
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
