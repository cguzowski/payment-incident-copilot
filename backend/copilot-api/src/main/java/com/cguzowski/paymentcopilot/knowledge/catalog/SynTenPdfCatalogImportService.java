package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SynTenPdfCatalogImportService {

    private final SynTenCorpusSourceRepository sources;
    private final PdfBoxKnowledgeDocumentParser parser;
    private final PdfKnowledgeChunker chunker;
    private final SynTenPdfCatalogPersistenceService persistence;
    private final Clock clock;

    SynTenPdfCatalogImportService(
            SynTenCorpusSourceRepository sources,
            PdfBoxKnowledgeDocumentParser parser,
            PdfKnowledgeChunker chunker,
            SynTenPdfCatalogPersistenceService persistence,
            Clock clock) {
        this.sources = sources;
        this.parser = parser;
        this.chunker = chunker;
        this.persistence = persistence;
        this.clock = clock;
    }

    public PdfCatalogImportSummary importCorpus() {
        Instant importedAt = Instant.now(clock);
        List<PdfCatalogDocumentPlan> plans = new ArrayList<>();
        for (SynTenPdfSourceDocument source : sources.findAll()) {
            PdfKnowledgeDocument document = parser.parse(source);
            List<PdfKnowledgeChunkDraft> chunks = chunker.chunk(document);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("SynTen PDF produced no catalog chunks: " + source.documentKey());
            }
            String catalogHash = catalogContentHash(document, chunks);
            plans.add(new PdfCatalogDocumentPlan(
                    stableId(source.tenantId() + "\u001f" + source.documentId() + "\u001f" + source.version() + "\u001f"
                            + catalogHash),
                    document,
                    catalogHash,
                    importedAt,
                    chunks));
        }
        return persistence.importAll(List.copyOf(plans));
    }

    private static String catalogContentHash(PdfKnowledgeDocument document, List<PdfKnowledgeChunkDraft> chunks) {
        SynTenPdfSourceDocument source = document.source();
        StringBuilder canonical = new StringBuilder()
                .append(source.tenantId())
                .append('\u001f')
                .append(source.documentId())
                .append('\u001f')
                .append(source.version())
                .append('\u001f')
                .append(source.sourceSha256())
                .append('\u001f')
                .append(document.pdfSha256())
                .append('\u001f')
                .append(document.extractionStrategyVersion())
                .append('\u001f')
                .append(PdfKnowledgeChunker.CHUNKING_STRATEGY_VERSION);
        for (PdfKnowledgeChunkDraft chunk : chunks) {
            canonical
                    .append('\u001e')
                    .append(chunk.ordinal())
                    .append('\u001f')
                    .append(chunk.pageNumber())
                    .append('\u001f')
                    .append(chunk.startBlock())
                    .append('\u001f')
                    .append(chunk.endBlock())
                    .append('\u001f')
                    .append(chunk.rawContentHash())
                    .append('\u001f')
                    .append(chunk.embeddingInputHash());
        }
        return sha256(canonical.toString());
    }

    private static UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
