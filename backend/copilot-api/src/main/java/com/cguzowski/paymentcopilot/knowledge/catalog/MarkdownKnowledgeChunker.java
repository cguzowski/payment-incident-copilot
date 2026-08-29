package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MarkdownKnowledgeChunker {

    static final String EMBEDDING_INPUT_TEMPLATE_VERSION = "embedding-input/v1";
    private static final Pattern BLOCK_SEPARATOR = Pattern.compile("(?:\\n[ \\t]*\\n)+");
    private static final Pattern WORD = Pattern.compile("\\S+");

    private final TokenEstimator tokenEstimator;
    private final int targetTokens;
    private final int hardMaximumTokens;
    private final int overlapTokens;

    MarkdownKnowledgeChunker(
            TokenEstimator tokenEstimator, int targetTokens, int hardMaximumTokens, int overlapTokens) {
        if (targetTokens <= 0
                || hardMaximumTokens < targetTokens
                || overlapTokens <= 0
                || overlapTokens >= targetTokens) {
            throw new IllegalArgumentException("Knowledge chunking limits are invalid.");
        }
        this.tokenEstimator = tokenEstimator;
        this.targetTokens = targetTokens;
        this.hardMaximumTokens = hardMaximumTokens;
        this.overlapTokens = overlapTokens;
    }

    List<KnowledgeChunkDraft> chunk(ApprovedKnowledgeDocument document) {
        List<SourceLine> lines = sourceLines(document.body());
        List<SectionContent> sections = sections(lines, document.title());
        List<KnowledgeChunkDraft> chunks = new ArrayList<>();
        for (SectionContent section : sections) {
            for (SectionContent chunkContent : split(section)) {
                int estimatedTokens = tokenEstimator.estimate(chunkContent.rawContent());
                if (estimatedTokens > hardMaximumTokens) {
                    throw new IllegalArgumentException("Knowledge chunk exceeds the hard maximum.");
                }
                chunks.add(draft(document, chunks.size(), chunkContent, estimatedTokens));
            }
        }
        return List.copyOf(chunks);
    }

    private List<SectionContent> split(SectionContent section) {
        String raw = section.rawContent();
        List<ContentRange> blocks = blocks(raw).stream()
                .flatMap(range -> splitOversizedBlock(raw, range).stream())
                .toList();
        if (blocks.isEmpty()) {
            return List.of();
        }

        List<SectionContent> chunks = new ArrayList<>();
        int currentStart = blocks.getFirst().start();
        int currentEnd = blocks.getFirst().end();
        for (int index = 1; index < blocks.size(); index++) {
            ContentRange block = blocks.get(index);
            if (tokenEstimator.estimate(raw.substring(currentStart, block.end())) <= targetTokens) {
                currentEnd = block.end();
                continue;
            }
            chunks.add(slice(section, currentStart, currentEnd));
            currentStart = overlapStart(raw, currentStart, currentEnd);
            currentEnd = block.end();
            currentStart = fitHardMaximum(raw, currentStart, currentEnd, block.start());
        }
        chunks.add(slice(section, currentStart, currentEnd));
        return chunks;
    }

    private List<ContentRange> splitOversizedBlock(String raw, ContentRange block) {
        if (tokenEstimator.estimate(raw.substring(block.start(), block.end())) <= hardMaximumTokens) {
            return List.of(block);
        }
        List<ContentRange> pieces = new ArrayList<>();
        Matcher words = WORD.matcher(raw.substring(block.start(), block.end()));
        int pieceStart = -1;
        int pieceEnd = -1;
        while (words.find()) {
            int wordStart = block.start() + words.start();
            int wordEnd = block.start() + words.end();
            if (pieceStart < 0) {
                pieceStart = wordStart;
            }
            if (pieceEnd >= 0 && tokenEstimator.estimate(raw.substring(pieceStart, wordEnd)) > targetTokens) {
                pieces.add(new ContentRange(pieceStart, pieceEnd));
                pieceStart = wordStart;
            }
            pieceEnd = wordEnd;
        }
        if (pieceStart >= 0) {
            pieces.add(new ContentRange(pieceStart, pieceEnd));
        }
        return pieces;
    }

    private int overlapStart(String raw, int chunkStart, int chunkEnd) {
        Matcher words = WORD.matcher(raw.substring(chunkStart, chunkEnd));
        List<Integer> starts = new ArrayList<>();
        while (words.find()) {
            starts.add(chunkStart + words.start());
        }
        int selected = chunkEnd;
        for (int index = starts.size() - 1; index >= 0; index--) {
            int candidate = starts.get(index);
            if (tokenEstimator.estimate(raw.substring(candidate, chunkEnd)) > overlapTokens) {
                break;
            }
            selected = candidate;
        }
        return selected;
    }

    private int fitHardMaximum(String raw, int candidateStart, int end, int blockStart) {
        if (tokenEstimator.estimate(raw.substring(candidateStart, end)) <= hardMaximumTokens) {
            return candidateStart;
        }
        Matcher words = WORD.matcher(raw.substring(candidateStart, blockStart));
        while (words.find()) {
            int nextStart = candidateStart + words.end();
            while (nextStart < blockStart && Character.isWhitespace(raw.charAt(nextStart))) {
                nextStart++;
            }
            if (tokenEstimator.estimate(raw.substring(nextStart, end)) <= hardMaximumTokens) {
                return nextStart;
            }
        }
        return blockStart;
    }

    private static List<ContentRange> blocks(String raw) {
        List<ContentRange> blocks = new ArrayList<>();
        Matcher separators = BLOCK_SEPARATOR.matcher(raw);
        int start = 0;
        while (separators.find()) {
            if (start < separators.start()) {
                blocks.add(new ContentRange(start, separators.start()));
            }
            start = separators.end();
        }
        if (start < raw.length()) {
            blocks.add(new ContentRange(start, raw.length()));
        }
        return blocks;
    }

    private static SectionContent slice(SectionContent section, int start, int end) {
        int startLineOffset = newlineCount(section.rawContent(), 0, start);
        int endLineOffset = newlineCount(section.rawContent(), 0, end);
        return new SectionContent(
                section.sectionPath(),
                section.startLineIndex() + startLineOffset,
                section.startLineIndex() + endLineOffset,
                section.rawContent().substring(start, end));
    }

    private static int newlineCount(String value, int start, int end) {
        int count = 0;
        for (int index = start; index < end; index++) {
            if (value.charAt(index) == '\n') {
                count++;
            }
        }
        return count;
    }

    private static KnowledgeChunkDraft draft(
            ApprovedKnowledgeDocument document, int ordinal, SectionContent section, int estimatedTokens) {
        String embeddingInput = "Document: " + document.title()
                + "\nSection: " + section.sectionPath()
                + "\nType: " + document.type()
                + "\nApplies to: " + document.appliesTo()
                + "\n\n" + section.rawContent();
        return new KnowledgeChunkDraft(
                ordinal,
                section.sectionPath(),
                section.rawContent(),
                embeddingInput,
                sha256(section.rawContent()),
                sha256(embeddingInput),
                EMBEDDING_INPUT_TEMPLATE_VERSION,
                document.bodyStartLine() + section.startLineIndex(),
                document.bodyStartLine() + section.endLineIndex(),
                estimatedTokens);
    }

    private static List<SectionContent> sections(List<SourceLine> lines, String documentTitle) {
        List<SectionContent> sections = new ArrayList<>();
        String[] headings = new String[6];
        int contentStart = 0;
        for (int index = 0; index < lines.size(); index++) {
            Heading heading = heading(lines.get(index).text());
            if (heading == null) {
                continue;
            }
            addSection(sections, lines, contentStart, index - 1, sectionPath(headings, documentTitle));
            headings[heading.level() - 1] = heading.title();
            for (int deeper = heading.level(); deeper < headings.length; deeper++) {
                headings[deeper] = null;
            }
            contentStart = heading.level() == 1 ? index + 1 : index;
        }
        addSection(sections, lines, contentStart, lines.size() - 1, sectionPath(headings, documentTitle));
        return sections;
    }

    private static void addSection(
            List<SectionContent> sections,
            List<SourceLine> lines,
            int candidateStart,
            int candidateEnd,
            String sectionPath) {
        int start = candidateStart;
        while (start <= candidateEnd && lines.get(start).text().isBlank()) {
            start++;
        }
        int end = candidateEnd;
        while (end >= start && lines.get(end).text().isBlank()) {
            end--;
        }
        if (start > end) {
            return;
        }
        if (start == end && heading(lines.get(start).text()) != null) {
            return;
        }
        StringBuilder raw = new StringBuilder();
        for (int index = start; index <= end; index++) {
            raw.append(lines.get(index).text());
            if (index < end) {
                raw.append('\n');
            }
        }
        sections.add(new SectionContent(sectionPath, start, end, raw.toString()));
    }

    private static String sectionPath(String[] headings, String documentTitle) {
        List<String> path = new ArrayList<>();
        for (int index = 1; index < headings.length; index++) {
            if (headings[index] != null) {
                path.add(headings[index]);
            }
        }
        return path.isEmpty() ? documentTitle : String.join(" > ", path);
    }

    private static Heading heading(String line) {
        int level = 0;
        while (level < line.length() && level < 6 && line.charAt(level) == '#') {
            level++;
        }
        if (level == 0 || level >= line.length() || line.charAt(level) != ' ') {
            return null;
        }
        String title = line.substring(level + 1).trim();
        return title.isEmpty() ? null : new Heading(level, title);
    }

    private static List<SourceLine> sourceLines(String source) {
        List<SourceLine> lines = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '\n') {
                lines.add(new SourceLine(source.substring(start, index), true));
                start = index + 1;
            }
        }
        if (start < source.length()) {
            lines.add(new SourceLine(source.substring(start), false));
        }
        return lines;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private record SourceLine(String text, boolean terminated) {}

    private record Heading(int level, String title) {}

    private record SectionContent(String sectionPath, int startLineIndex, int endLineIndex, String rawContent) {}

    private record ContentRange(int start, int end) {}
}
