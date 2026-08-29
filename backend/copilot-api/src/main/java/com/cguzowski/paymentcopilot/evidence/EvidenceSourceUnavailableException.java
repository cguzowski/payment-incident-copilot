package com.cguzowski.paymentcopilot.evidence;

final class EvidenceSourceUnavailableException extends RuntimeException {

    EvidenceSourceUnavailableException() {
        super("The evidence source is unavailable.");
    }

    EvidenceSourceUnavailableException(Throwable cause) {
        super("The evidence source is unavailable.", cause);
    }
}
