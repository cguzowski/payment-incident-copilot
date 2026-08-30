package com.cguzowski.paymentcopilot.decision;

final class InvalidHumanDecisionRequestException extends RuntimeException {

    private final String field;

    InvalidHumanDecisionRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    String field() {
        return field;
    }
}
