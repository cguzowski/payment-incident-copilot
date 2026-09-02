package com.cguzowski.paymentcopilot.knowledge.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PdfBoxKnowledgeDocumentParserTest {

    private static final Path CORPUS_ROOT =
            Path.of("..", "..", "SynTen Inc", "corpus").toAbsolutePath().normalize();

    private final List<SynTenPdfSourceDocument> sources =
            new SynTenCorpusSourceRepository(CORPUS_ROOT, JsonMapper.builder().build()).findAll();
    private final PdfBoxKnowledgeDocumentParser parser = new PdfBoxKnowledgeDocumentParser();

    @Test
    void extractsDeterministicPageBlocksAndRemovesOnlyGeneratedMargins() {
        SynTenPdfSourceDocument source = source("RB-002");

        PdfKnowledgeDocument first = parser.parse(source);
        PdfKnowledgeDocument second = parser.parse(source);

        assertThat(first).isEqualTo(second);
        assertThat(first.extractionStrategyVersion()).isEqualTo("pdfbox-text-pages/v1");
        assertThat(first.pdfSha256()).isEqualTo(source.pdfSha256());
        assertThat(first.pages()).hasSize(4);
        assertThat(first.pages()).extracting(PdfKnowledgePage::pageNumber).containsExactly(1, 2, 3, 4);
        assertThat(first.pages()).allSatisfy(page -> {
            assertThat(page.blocks()).isNotEmpty();
            assertThat(page.blocks())
                    .extracting(PdfTextBlock::ordinal)
                    .containsExactly(java.util.stream.IntStream.rangeClosed(
                                    1, page.blocks().size())
                            .boxed()
                            .toArray(Integer[]::new));
            assertThat(page.text())
                    .doesNotContain("RB-002 | Gateway Connectivity and Timeout Runbook")
                    .doesNotContain("SynTen Inc - Internal - Synthetic Demo")
                    .doesNotContain("| Page " + page.pageNumber() + " of 4");
        });
        assertThat(first.pages().get(2).text()).contains("5. Diagnostic procedure", "6. Scenario decision matrix");
    }

    @Test
    void preservesPolicyTableOrderDenseRunbookTablesAndSupersededBanners() {
        PdfKnowledgeDocument policy = parser.parse(source("PL-001"));
        PdfKnowledgeDocument denseRunbook = parser.parse(source("RB-011"));
        PdfKnowledgeDocument superseded = parser.parse(source("RB-022"));

        assertThat(policy.pages().get(1).text())
                .containsSubsequence(
                        "Document control", "Control Value", "Revision history", "Version Status Date Summary");
        assertThat(policy.pages().get(2).text()).contains("5. Roles and responsibilities", "6. Evidence and records");
        assertThat(denseRunbook.pages().get(2).text())
                .containsSubsequence(
                        "Exact signal Bounded interpretation Required caution",
                        "BGP_ROUTE_UNREACHABLE",
                        "DNS_RESOLUTION_FAILED",
                        "5. Diagnostic procedure");
        assertThat(superseded.pages())
                .allSatisfy(page -> assertThat(page.text())
                        .contains("SUPERSEDED - NOT RETRIEVAL ELIGIBLE", "Approved replacement: RB-002 and RB-006"));
    }

    @Test
    void normalizesLineEndingsUnicodeAndHorizontalWhitespace() {
        assertThat(PdfBoxKnowledgeDocumentParser.normalizeLine("  Cafe\u0301\u00a0\t  signal\r"))
                .isEqualTo("Café signal");
    }

    @Test
    void rejectsMalformedEncryptedEmptyOverLimitAndWrongMargins() throws IOException {
        SynTenPdfSourceDocument source = source("RB-002");

        assertThatThrownBy(() -> parser.parse(withPdf(source, new byte[] {1, 2, 3}, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF is malformed: RB-002");

        byte[] encrypted = encryptedPdf(validPageLines(source, 1, 1, "Body"));
        assertThatThrownBy(() -> parser.parse(withPdf(source, encrypted, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF is encrypted or password-protected: RB-002");

        byte[] empty = pdf(List.of(validPageLines(source, 1, 1)));
        assertThatThrownBy(() -> parser.parse(withPdf(source, empty, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF page has no extractable content: RB-002 (page 1)");

        byte[] zeroPages = pdf(List.of());
        assertThatThrownBy(() -> parser.parse(withPdf(source, zeroPages, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF page count is outside 1-15: RB-002");

        byte[] tooManyPages = pdf(java.util.stream.IntStream.rangeClosed(1, 16)
                .mapToObj(page -> validPageLines(source, page, 16, "Body " + page))
                .toList());
        assertThatThrownBy(() -> parser.parse(withPdf(source, tooManyPages, 16)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF page count is outside 1-15: RB-002");

        byte[] wrongMargin = pdf(List.of(List.of("Unexpected header", "Unexpected footer", "Body")));
        assertThatThrownBy(() -> parser.parse(withPdf(source, wrongMargin, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF generated header/footer does not match: RB-002 (page 1)");
    }

    @Test
    void rejectsManifestPageCountMismatch() throws IOException {
        SynTenPdfSourceDocument source = source("RB-002");
        byte[] onePage = pdf(List.of(validPageLines(source, 1, 1, "Body")));

        assertThatThrownBy(() -> parser.parse(withPdf(source, onePage, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SynTen PDF page count does not match the manifest: RB-002");
    }

    private SynTenPdfSourceDocument source(String key) {
        return sources.stream()
                .filter(document -> document.documentKey().equals(key))
                .findFirst()
                .orElseThrow();
    }

    private static SynTenPdfSourceDocument withPdf(
            SynTenPdfSourceDocument source, byte[] pdfBytes, int manifestPageCount) {
        return new SynTenPdfSourceDocument(
                source.documentKey(),
                source.documentId(),
                source.tenantId(),
                source.type(),
                source.title(),
                source.version(),
                source.incidentFamily(),
                source.appliesTo(),
                source.approvalStatus(),
                source.approvedBy(),
                source.approvedAt(),
                source.effectiveAt(),
                source.classification(),
                source.replacement(),
                source.sourceName(),
                source.pdfName(),
                source.sourceSha256(),
                sha256(pdfBytes),
                manifestPageCount,
                source.sourceBytes(),
                pdfBytes);
    }

    private static List<String> validPageLines(
            SynTenPdfSourceDocument source, int page, int pageCount, String... content) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add(source.documentKey() + " | " + source.title() + " v" + source.version() + " | "
                + source.classification());
        lines.add("SynTen Inc - " + source.classification() + " " + source.documentId() + " | Page " + page + " of "
                + pageCount);
        lines.addAll(List.of(content));
        return List.copyOf(lines);
    }

    private static byte[] pdf(List<List<String>> pages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (List<String> lines : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    content.newLineAtOffset(40, 740);
                    for (String line : lines) {
                        content.showText(line);
                        content.newLineAtOffset(0, -14);
                    }
                    content.endText();
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static byte[] encryptedPdf(List<String> lines) throws IOException {
        byte[] unencrypted = pdf(List.of(lines));
        try (PDDocument document = Loader.loadPDF(unencrypted)) {
            StandardProtectionPolicy policy =
                    new StandardProtectionPolicy("owner-password", "user-password", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
