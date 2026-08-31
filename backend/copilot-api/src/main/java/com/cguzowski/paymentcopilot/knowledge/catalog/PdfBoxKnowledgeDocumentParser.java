package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
final class PdfBoxKnowledgeDocumentParser {

    static final String EXTRACTION_STRATEGY_VERSION = "pdfbox-text-pages/v1";
    private static final int MAXIMUM_PAGES = 15;
    private static final Pattern HORIZONTAL_WHITESPACE = Pattern.compile("[\\p{Zs}\\t]+");

    PdfKnowledgeDocument parse(SynTenPdfSourceDocument source) {
        try (PDDocument document = load(source)) {
            if (document.isEncrypted()) {
                throw invalid("SynTen PDF is encrypted or password-protected: " + source.documentKey());
            }
            int pageCount = document.getNumberOfPages();
            if (pageCount < 1 || pageCount > MAXIMUM_PAGES) {
                throw invalid("SynTen PDF page count is outside 1-15: " + source.documentKey());
            }
            if (pageCount != source.manifestPageCount()) {
                throw invalid("SynTen PDF page count does not match the manifest: " + source.documentKey());
            }

            List<PdfKnowledgePage> pages = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                pages.add(extractPage(document, source, pageNumber, pageCount));
            }
            return new PdfKnowledgeDocument(
                    source, source.pdfSha256(), EXTRACTION_STRATEGY_VERSION, List.copyOf(pages));
        } catch (InvalidPasswordException exception) {
            throw invalid("SynTen PDF is encrypted or password-protected: " + source.documentKey(), exception);
        } catch (IOException exception) {
            throw invalid("SynTen PDF is malformed: " + source.documentKey(), exception);
        }
    }

    private static PDDocument load(SynTenPdfSourceDocument source) throws IOException {
        return Loader.loadPDF(source.pdfBytes());
    }

    private static PdfKnowledgePage extractPage(
            PDDocument document, SynTenPdfSourceDocument source, int pageNumber, int pageCount) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        List<String> lines = stripper.getText(document)
                .lines()
                .map(PdfBoxKnowledgeDocumentParser::normalizeLine)
                .filter(line -> !line.isBlank())
                .toList();

        String expectedHeader = normalizeLine(source.documentKey() + " | " + source.title() + " v" + source.version()
                + " | " + source.classification());
        String expectedFooter = normalizeLine("SynTen Inc - " + source.classification() + " " + source.documentId()
                + " | Page " + pageNumber + " of " + pageCount);
        int footerIndex = lines.indexOf(expectedFooter);
        if (lines.size() < 2 || !lines.get(0).equals(expectedHeader) || footerIndex < 1 || footerIndex > 2) {
            throw invalid("SynTen PDF generated header/footer does not match: " + source.documentKey() + " (page "
                    + pageNumber + ")");
        }

        List<String> content = new ArrayList<>(lines.subList(1, lines.size()));
        content.remove(footerIndex - 1);
        if (content.isEmpty()) {
            throw invalid("SynTen PDF page has no extractable content: " + source.documentKey() + " (page " + pageNumber
                    + ")");
        }
        List<PdfTextBlock> blocks = new ArrayList<>();
        for (int index = 0; index < content.size(); index++) {
            blocks.add(new PdfTextBlock(index + 1, content.get(index)));
        }
        return new PdfKnowledgePage(pageNumber, blocks);
    }

    static String normalizeLine(String value) {
        String normalized = Normalizer.normalize(value.replace('\u00a0', ' '), Normalizer.Form.NFC);
        return HORIZONTAL_WHITESPACE.matcher(normalized).replaceAll(" ").strip();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException(message, cause);
    }
}
