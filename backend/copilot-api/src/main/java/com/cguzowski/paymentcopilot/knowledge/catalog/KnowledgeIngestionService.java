package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeIngestionService {

    private final ApprovedKnowledgeSourceRepository sources;
    private final MarkdownKnowledgeChunker chunker;
    private final KnowledgeEmbeddingClient embeddingClient;
    private final KnowledgeIndexPersistenceService persistence;
    private final Clock clock;

    KnowledgeIngestionService(
            ApprovedKnowledgeSourceRepository sources,
            MarkdownKnowledgeChunker chunker,
            KnowledgeEmbeddingClient embeddingClient,
            KnowledgeIndexPersistenceService persistence,
            Clock clock) {
        this.sources = sources;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.persistence = persistence;
        this.clock = clock;
    }

    public KnowledgeImportSummary importApprovedSources() {
        int importedDocuments = 0;
        int skippedDocuments = 0;
        int embeddedChunks = 0;
        for (ApprovedKnowledgeDocument document : sources.findAll()) {
            if (document.approvalStatus() != KnowledgeApprovalStatus.APPROVED) {
                throw new IllegalArgumentException("Only approved knowledge sources can be imported.");
            }
            String sourceHash = KnowledgeSourceHasher.hash(document);
            Optional<String> existingHash =
                    persistence.findSourceContentHash(document.tenantId(), document.documentId(), document.version());
            if (existingHash.isPresent()) {
                if (!existingHash.orElseThrow().equals(sourceHash)) {
                    throw new IllegalArgumentException(
                            "Changed knowledge content or metadata requires a new document version.");
                }
                skippedDocuments++;
                continue;
            }

            Instant indexedAt = Instant.now(clock);
            List<IndexedKnowledgeChunk> indexedChunks = new ArrayList<>();
            for (KnowledgeChunkDraft draft : chunker.chunk(document)) {
                KnowledgeEmbedding embedding = embeddingClient.embed(draft.embeddingInput());
                indexedChunks.add(new IndexedKnowledgeChunk(
                        stableId(document.documentId() + "\u001f" + document.version() + "\u001f" + sourceHash
                                + "\u001f" + draft.ordinal()),
                        draft,
                        embedding,
                        indexedAt));
            }
            IndexedKnowledgeDocument indexed = new IndexedKnowledgeDocument(
                    stableId(document.tenantId() + "\u001f" + document.documentId() + "\u001f" + document.version()
                            + "\u001f" + sourceHash),
                    document,
                    sourceHash,
                    indexedAt,
                    indexedChunks);
            if (persistence.insert(indexed)) {
                importedDocuments++;
                embeddedChunks += indexedChunks.size();
            } else {
                skippedDocuments++;
            }
        }
        return new KnowledgeImportSummary(importedDocuments, skippedDocuments, embeddedChunks);
    }

    private static UUID stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
