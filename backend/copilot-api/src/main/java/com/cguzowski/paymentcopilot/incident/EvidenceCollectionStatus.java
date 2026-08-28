package com.cguzowski.paymentcopilot.incident;

public enum EvidenceCollectionStatus {
    STARTED,
    AVAILABLE,
    PARTIAL,
    NOT_FOUND,
    UNAVAILABLE,
    TIMED_OUT,
    MALFORMED;

    boolean isTerminal() {
        return this != STARTED;
    }
}
