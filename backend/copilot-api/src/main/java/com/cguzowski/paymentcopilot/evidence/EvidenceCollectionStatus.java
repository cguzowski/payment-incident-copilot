package com.cguzowski.paymentcopilot.evidence;

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
