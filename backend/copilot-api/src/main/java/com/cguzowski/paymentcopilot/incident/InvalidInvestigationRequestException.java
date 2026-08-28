package com.cguzowski.paymentcopilot.incident;

class InvalidInvestigationRequestException extends RuntimeException {

    private final String field;

    InvalidInvestigationRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    String field() {
        return field;
    }
}
