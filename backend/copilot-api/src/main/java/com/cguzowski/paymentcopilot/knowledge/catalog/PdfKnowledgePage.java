package com.cguzowski.paymentcopilot.knowledge.catalog;

import java.util.List;

record PdfKnowledgePage(int pageNumber, List<PdfTextBlock> blocks) {

    PdfKnowledgePage {
        blocks = List.copyOf(blocks);
    }

    String text() {
        return blocks.stream().map(PdfTextBlock::text).collect(java.util.stream.Collectors.joining("\n"));
    }
}
