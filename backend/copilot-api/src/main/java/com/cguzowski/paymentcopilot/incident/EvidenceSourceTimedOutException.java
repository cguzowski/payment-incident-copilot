package com.cguzowski.paymentcopilot.incident;

final class EvidenceSourceTimedOutException extends RuntimeException {

    EvidenceSourceTimedOutException() {
        super("The evidence source request timed out.");
    }

    EvidenceSourceTimedOutException(Throwable cause) {
        super("The evidence source request timed out.", cause);
    }
}
