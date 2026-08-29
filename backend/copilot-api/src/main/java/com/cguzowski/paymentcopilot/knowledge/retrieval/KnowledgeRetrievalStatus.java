package com.cguzowski.paymentcopilot.knowledge.retrieval;

public enum KnowledgeRetrievalStatus {
    STARTED,
    AVAILABLE,
    PARTIAL,
    NO_MATCH,
    UNAVAILABLE,
    TIMED_OUT,
    MALFORMED;

    boolean isTerminal() {
        return this != STARTED;
    }
}
