package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PdfKnowledgeChunker {

    static final String CHUNKING_STRATEGY_VERSION = "pdf-page-sections/v1";
    private static final Pattern NUMBERED_HEADING = Pattern.compile("^\\d+(?:\\.\\d+)*\\.\\s+(.+)$");
    private static final Pattern WORD = Pattern.compile("\\S+");
    private static final Set<String> NAMED_HEADINGS = Set.of("Document control", "Revision history");

    private final TokenEstimator tokenEstimator;
    private final int targetTokens;
    private final int hardMaximumTokens;
    private final int overlapTokens;
    private final int preferredMinimumTokens;

    PdfKnowledgeChunker(
            TokenEstimator tokenEstimator,
            int targetTokens,
            int hardMaximumTokens,
            int overlapTokens,
            int preferredMinimumTokens) {
        if (targetTokens <= 0
                || hardMaximumTokens < targetTokens
                || overlapTokens <= 0
                || overlapTokens >= targetTokens
                || preferredMinimumTokens <= 0
                || preferredMinimumTokens > targetTokens) {
            throw new IllegalArgumentException("PDF knowledge chunking limits are invalid.");
        }
        this.tokenEstimator = tokenEstimator;
        this.targetTokens = targetTokens;
        this.hardMaximumTokens = hardMaximumTokens;
        this.overlapTokens = overlapTokens;
        this.preferredMinimumTokens = preferredMinimumTokens;
    }

    List<PdfKnowledgeChunkDraft> chunk(PdfKnowledgeDocument document) {
        List<ChunkContent> content = new ArrayList<>();
        String currentSection = document.source().title();
        for (PdfKnowledgePage page : document.pages()) {
            List<SectionUnits> sections = new ArrayList<>();
            List<Unit> units = new ArrayList<>();
            String section = currentSection;
            for (PdfTextBlock block : page.blocks()) {
                String heading = heading(block.text());
                if (heading != null) {
                    addSection(sections, section, units);
                    units = new ArrayList<>();
                    currentSection = heading;
                    section = heading;
                }
                units.addAll(splitOversized(new Unit(block.ordinal(), block.ordinal(), block.text())));
            }
            addSection(sections, section, units);
            for (SectionUnits sectionUnits : sections) {
                content.addAll(split(page.pageNumber(), sectionUnits));
            }
        }

        List<PdfKnowledgeChunkDraft> drafts = new ArrayList<>();
        for (ChunkContent chunk : content) {
            drafts.add(draft(document, drafts.size(), chunk));
        }
        return List.copyOf(drafts);
    }

    private List<ChunkContent> split(int pageNumber, SectionUnits section) {
        List<ChunkContent> chunks = new ArrayList<>();
        List<Unit> current = new ArrayList<>();
        for (Unit unit : section.units()) {
            if (current.isEmpty()) {
                current.add(unit);
                continue;
            }
            int currentTokens = tokens(current);
            int candidateTokens = tokens(with(current, unit));
            if (currentTokens < targetTokens && candidateTokens <= hardMaximumTokens) {
                current.add(unit);
                continue;
            }
            chunks.add(content(pageNumber, section.path(), current));
            current = overlap(current);
            while (!current.isEmpty() && tokens(with(current, unit)) > hardMaximumTokens) {
                current.removeFirst();
            }
            current.add(unit);
        }
        if (!current.isEmpty()) {
            chunks.add(content(pageNumber, section.path(), current));
        }
        mergeShortTail(chunks);
        return List.copyOf(chunks);
    }

    private void mergeShortTail(List<ChunkContent> chunks) {
        if (chunks.size() < 2 || chunks.getLast().estimatedTokens() >= preferredMinimumTokens) {
            return;
        }
        ChunkContent previous = chunks.get(chunks.size() - 2);
        ChunkContent tail = chunks.getLast();
        List<Unit> mergedUnits = distinctUnits(previous.units(), tail.units());
        if (tokens(mergedUnits) <= hardMaximumTokens) {
            chunks.set(chunks.size() - 2, content(previous.pageNumber(), previous.sectionPath(), mergedUnits));
            chunks.removeLast();
        }
    }

    private List<Unit> splitOversized(Unit block) {
        if (tokenEstimator.estimate(block.text()) <= hardMaximumTokens) {
            return List.of(block);
        }
        List<Unit> pieces = new ArrayList<>();
        Matcher words = WORD.matcher(block.text());
        StringBuilder current = new StringBuilder();
        while (words.find()) {
            String word = words.group();
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && tokenEstimator.estimate(candidate) > targetTokens) {
                pieces.add(new Unit(block.startBlock(), block.endBlock(), current.toString()));
                current = new StringBuilder(word);
            } else {
                if (!current.isEmpty()) {
                    current.append(' ');
                }
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            pieces.add(new Unit(block.startBlock(), block.endBlock(), current.toString()));
        }
        return List.copyOf(pieces);
    }

    private List<Unit> overlap(List<Unit> units) {
        List<Unit> selected = new ArrayList<>();
        int tokens = 0;
        for (int index = units.size() - 1; index >= 0; index--) {
            Unit candidate = units.get(index);
            int candidateTokens = tokenEstimator.estimate(candidate.text());
            if (!selected.isEmpty() && tokens + candidateTokens > overlapTokens) {
                break;
            }
            selected.addFirst(candidate);
            tokens += candidateTokens;
            if (tokens >= overlapTokens) {
                break;
            }
        }
        return selected;
    }

    private ChunkContent content(int pageNumber, String sectionPath, List<Unit> units) {
        String raw = units.stream().map(Unit::text).collect(java.util.stream.Collectors.joining("\n"));
        int estimatedTokens = tokenEstimator.estimate(raw);
        if (estimatedTokens > hardMaximumTokens) {
            throw new IllegalArgumentException("PDF knowledge chunk exceeds the hard maximum.");
        }
        return new ChunkContent(
                pageNumber,
                sectionPath,
                List.copyOf(units),
                units.stream().mapToInt(Unit::startBlock).min().orElseThrow(),
                units.stream().mapToInt(Unit::endBlock).max().orElseThrow(),
                raw,
                estimatedTokens);
    }

    private PdfKnowledgeChunkDraft draft(PdfKnowledgeDocument document, int ordinal, ChunkContent chunk) {
        SynTenPdfSourceDocument source = document.source();
        String embeddingInput = "Document: " + source.title()
                + "\nSection: " + chunk.sectionPath()
                + "\nType: " + source.type()
                + "\nApplies to: " + source.appliesTo()
                + "\n\n" + chunk.rawContent();
        String rawHash = sha256(chunk.rawContent());
        String embeddingHash = sha256(embeddingInput);
        String identity = source.tenantId() + "\u001f" + source.documentId() + "\u001f" + source.version() + "\u001f"
                + document.pdfSha256() + "\u001f" + document.extractionStrategyVersion() + "\u001f"
                + CHUNKING_STRATEGY_VERSION + "\u001f" + ordinal + "\u001f" + chunk.pageNumber() + "\u001f"
                + chunk.startBlock() + "\u001f" + chunk.endBlock() + "\u001f" + rawHash;
        return new PdfKnowledgeChunkDraft(
                UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)),
                ordinal,
                chunk.sectionPath(),
                chunk.rawContent(),
                embeddingInput,
                rawHash,
                embeddingHash,
                MarkdownKnowledgeChunker.EMBEDDING_INPUT_TEMPLATE_VERSION,
                CHUNKING_STRATEGY_VERSION,
                chunk.pageNumber(),
                chunk.startBlock(),
                chunk.endBlock(),
                chunk.estimatedTokens());
    }

    private static String heading(String text) {
        Matcher matcher = NUMBERED_HEADING.matcher(text);
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return NAMED_HEADINGS.contains(text) ? text : null;
    }

    private static void addSection(List<SectionUnits> sections, String path, List<Unit> units) {
        if (!units.isEmpty()) {
            sections.add(new SectionUnits(path, List.copyOf(units)));
        }
    }

    private int tokens(List<Unit> units) {
        return tokenEstimator.estimate(
                units.stream().map(Unit::text).collect(java.util.stream.Collectors.joining("\n")));
    }

    private static List<Unit> with(List<Unit> units, Unit next) {
        List<Unit> combined = new ArrayList<>(units);
        combined.add(next);
        return combined;
    }

    private static List<Unit> distinctUnits(List<Unit> first, List<Unit> second) {
        LinkedHashSet<Unit> units = new LinkedHashSet<>(first);
        units.addAll(second);
        return List.copyOf(units);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private record Unit(int startBlock, int endBlock, String text) {}

    private record SectionUnits(String path, List<Unit> units) {}

    private record ChunkContent(
            int pageNumber,
            String sectionPath,
            List<Unit> units,
            int startBlock,
            int endBlock,
            String rawContent,
            int estimatedTokens) {}
}
