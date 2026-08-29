package com.cguzowski.paymentcopilot.knowledge.retrieval;

final class InvalidKnowledgeRetrievalRequestException extends RuntimeException {

    private final String field;

    InvalidKnowledgeRetrievalRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    String field() {
        return field;
    }
}
