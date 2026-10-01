package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class PaymentPdfCatalogTest {
    static final Path ROOT = Path.of("..", "..", "SynTen Inc", "payment-knowledge", "v1")
            .toAbsolutePath()
            .normalize();

    static SynTenPdfCatalogPlanner planner(Path root) {
        return new SynTenPdfCatalogPlanner(
                new SynTenCorpusSourceRepository(root, JsonMapper.builder().build()),
                new PdfBoxKnowledgeDocumentParser(),
                new PdfKnowledgeChunker(new ApproximateTokenEstimator(), 400, 600, 50, 80));
    }

    @Test
    void plansFrozenLibraryDeterministicallyWithExactPdfLocators() {
        var first = planner(ROOT).plan();
        var second = planner(ROOT).plan();
        assertThat(first.documents()).hasSize(16);
        assertThat(first.catalogFingerprint()).isEqualTo(second.catalogFingerprint());
        assertThat(first.documents().stream().flatMap(d -> d.chunks().stream()).toList())
                .containsExactlyElementsOf(second.documents().stream()
                        .flatMap(d -> d.chunks().stream())
                        .toList());
        assertThat(first.documents()).allSatisfy(d -> {
            assertThat(d.document().source().pdfSha256()).matches("[a-f0-9]{64}");
            assertThat(d.chunks()).allSatisfy(c -> {
                assertThat(c.pageNumber()).isBetween(1, 3);
                assertThat(c.startBlock()).isPositive();
                assertThat(c.endBlock()).isGreaterThanOrEqualTo(c.startBlock());
                assertThat(c.estimatedTokens()).isLessThanOrEqualTo(600);
                assertThat(c.rawContent()).doesNotContain("SYNTEN INC /", "Page 1 of 3");
            });
        });
    }

    @Test
    void missingPdfFailsBeforePlanning(@TempDir Path directory) throws Exception {
        copyLibrary(directory);
        Files.delete(directory.resolve("pdfs/rb-101-authorization-rejection-establish-the-source.pdf"));
        assertThatThrownBy(() -> planner(directory).plan()).hasMessageContaining("PDF");
    }

    @Test
    void changedPdfAndUnacceptedManifestFailBeforePlanning(@TempDir Path directory) throws Exception {
        copyLibrary(directory);
        var pdf = directory.resolve("pdfs/rb-101-authorization-rejection-establish-the-source.pdf");
        Files.writeString(pdf, "%PDF changed");
        assertThatThrownBy(() -> planner(directory).plan()).hasMessageContaining("PDF hash");
        Files.writeString(directory.resolve("freeze-manifest.json"), "{}");
        assertThatThrownBy(() -> planner(directory).plan()).hasMessageContaining("Unaccepted");
    }

    @Test
    void changedSourceFailsBeforePlanning(@TempDir Path directory) throws Exception {
        copyLibrary(directory);
        Files.writeString(
                directory.resolve("sources/rb-101-authorization-rejection-establish-the-source.md"), "changed");
        assertThatThrownBy(() -> planner(directory).plan()).hasMessageContaining("hash");
    }

    private static void copyLibrary(Path target) throws Exception {
        try (var files = Files.walk(ROOT)) {
            for (Path source : files.toList()) {
                Path destination = target.resolve(ROOT.relativize(source));
                if (Files.isDirectory(source)) Files.createDirectories(destination);
                else Files.copy(source, destination);
            }
        }
    }
}
