package com.cguzowski.paymentcopilot.incident;

final class InvalidIncidentRequestException extends RuntimeException {

    private final String field;

    InvalidIncidentRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    String field() {
        return field;
    }
}
