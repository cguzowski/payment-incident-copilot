package com.cguzowski.paymentcopilot.knowledge.catalog;

record PdfKnowledgeArtifact(String sha256, String fileName, byte[] content) {

    PdfKnowledgeArtifact {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
