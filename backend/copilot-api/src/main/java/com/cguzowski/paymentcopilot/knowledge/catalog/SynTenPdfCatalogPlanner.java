package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class SynTenPdfCatalogPlanner {

    private static final String PLAN_VERSION = "synten-pdf-catalog-plan/v1";
    static final int EXPECTED_DOCUMENT_COUNT = 30;
    static final int EXPECTED_CHUNK_COUNT = 705;
    static final String ACCEPTED_CATALOG_FINGERPRINT =
            "5d704fee24f9754176f1be2e449050190e0b78ffa3fdb9ddc92b464006d198e9";
    static final String PAYMENT_CATALOG_FINGERPRINT =
            "d4e008fceb8b765cb644e45be14c71753fb3a4220f473d33973652578f164f46";
    static final int PAYMENT_CHUNK_COUNT = 65;

    static int acceptedChunkCount(String fingerprint) {
        if (ACCEPTED_CATALOG_FINGERPRINT.equals(fingerprint)) return EXPECTED_CHUNK_COUNT;
        if (PAYMENT_CATALOG_FINGERPRINT.equals(fingerprint)) return PAYMENT_CHUNK_COUNT;
        throw new IllegalArgumentException("Unaccepted PDF catalog fingerprint.");
    }

    private final SynTenCorpusSourceRepository sources;
    private final PdfBoxKnowledgeDocumentParser parser;
    private final PdfKnowledgeChunker chunker;

    SynTenPdfCatalogPlanner(
            SynTenCorpusSourceRepository sources, PdfBoxKnowledgeDocumentParser parser, PdfKnowledgeChunker chunker) {
        this.sources = sources;
        this.parser = parser;
        this.chunker = chunker;
    }

    SynTenPdfCatalogPlan plan() {
        List<PdfCatalogDocumentPlan> documents = new ArrayList<>();
        UUID tenantId = null;
        for (SynTenPdfSourceDocument source : sources.findAll()) {
            if (tenantId == null) {
                tenantId = source.tenantId();
            } else if (!tenantId.equals(source.tenantId())) {
                throw new IllegalArgumentException("SynTen PDF catalog contains more than one tenant.");
            }
            PdfKnowledgeDocument document = parser.parse(source);
            List<PdfKnowledgeChunkDraft> chunks = chunker.chunk(document);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("SynTen PDF produced no catalog chunks: " + source.documentKey());
            }
            String catalogHash = catalogContentHash(document, chunks);
            documents.add(new PdfCatalogDocumentPlan(
                    stableId(source.tenantId() + "\u001f" + source.documentId() + "\u001f" + source.version() + "\u001f"
                            + catalogHash),
                    document,
                    catalogHash,
                    chunks));
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("SynTen PDF catalog is empty.");
        }
        List<PdfCatalogDocumentPlan> immutableDocuments = List.copyOf(documents);
        SynTenPdfCatalogPlan plan = new SynTenPdfCatalogPlan(
                tenantId, catalogFingerprint(tenantId, immutableDocuments), immutableDocuments);
        if (plan.documents().getFirst().document().source().independentPaymentLibrary()) {
            if (plan.documents().size() != 16
                    || plan.chunkCount() != PAYMENT_CHUNK_COUNT
                    || !plan.catalogFingerprint().equals(PAYMENT_CATALOG_FINGERPRINT)) {
                throw new IllegalArgumentException("Payment catalog differs from the accepted plan.");
            }
            return plan;
        }
        if (plan.documents().size() != EXPECTED_DOCUMENT_COUNT
                || plan.chunkCount() != EXPECTED_CHUNK_COUNT
                || !plan.catalogFingerprint().equals(ACCEPTED_CATALOG_FINGERPRINT)) {
            throw new IllegalArgumentException("SynTen PDF catalog differs from the accepted K3 plan: documents="
                    + plan.documents().size() + ", chunks=" + plan.chunkCount() + ", fingerprint="
                    + plan.catalogFingerprint() + ".");
        }
        return plan;
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

    private static String catalogFingerprint(UUID tenantId, List<PdfCatalogDocumentPlan> documents) {
        StringBuilder canonical =
                new StringBuilder(PLAN_VERSION).append('\u001f').append(tenantId);
        for (PdfCatalogDocumentPlan plan : documents) {
            SynTenPdfSourceDocument source = plan.document().source();
            canonical
                    .append('\u001d')
                    .append(source.documentKey())
                    .append('\u001f')
                    .append(plan.documentVersionId())
                    .append('\u001f')
                    .append(source.documentId())
                    .append('\u001f')
                    .append(source.type())
                    .append('\u001f')
                    .append(source.title())
                    .append('\u001f')
                    .append(source.version())
                    .append('\u001f')
                    .append(source.incidentFamily())
                    .append('\u001f')
                    .append(source.appliesTo())
                    .append('\u001f')
                    .append(source.approvalStatus())
                    .append('\u001f')
                    .append(source.approvedBy())
                    .append('\u001f')
                    .append(source.approvedAt())
                    .append('\u001f')
                    .append(source.effectiveAt())
                    .append('\u001f')
                    .append(source.pdfName())
                    .append('\u001f')
                    .append(source.sourceSha256())
                    .append('\u001f')
                    .append(source.pdfSha256())
                    .append('\u001f')
                    .append(plan.document().extractionStrategyVersion())
                    .append('\u001f')
                    .append(plan.catalogContentHash());
            for (PdfKnowledgeChunkDraft chunk : plan.chunks()) {
                canonical
                        .append('\u001e')
                        .append(chunk.chunkId())
                        .append('\u001f')
                        .append(chunk.ordinal())
                        .append('\u001f')
                        .append(chunk.rawContentHash())
                        .append('\u001f')
                        .append(chunk.embeddingInputHash())
                        .append('\u001f')
                        .append(chunk.embeddingInputTemplateVersion())
                        .append('\u001f')
                        .append(chunk.chunkingStrategyVersion())
                        .append('\u001f')
                        .append(chunk.pageNumber())
                        .append('\u001f')
                        .append(chunk.startBlock())
                        .append('\u001f')
                        .append(chunk.endBlock());
            }
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
