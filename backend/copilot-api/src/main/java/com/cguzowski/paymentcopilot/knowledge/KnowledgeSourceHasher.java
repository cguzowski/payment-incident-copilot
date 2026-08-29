package com.cguzowski.paymentcopilot.knowledge;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class KnowledgeSourceHasher {

    private KnowledgeSourceHasher() {
    }

    static String hash(ApprovedKnowledgeDocument document) {
        String canonical = String.join("\u001f",
                document.documentId().toString(),
                document.tenantId().toString(),
                document.type().name(),
                document.title(),
                document.version(),
                document.incidentFamily(),
                document.appliesTo(),
                document.approvalStatus().name(),
                document.approvedBy().toString(),
                document.approvedAt().toString(),
                document.effectiveAt().toString(),
                document.sourceName(),
                Integer.toString(document.bodyStartLine()),
                document.body());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
