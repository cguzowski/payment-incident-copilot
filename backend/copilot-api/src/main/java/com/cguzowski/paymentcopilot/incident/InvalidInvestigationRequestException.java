package com.cguzowski.paymentcopilot.incident;

public class InvalidInvestigationRequestException extends RuntimeException {

    private final String field;

    public InvalidInvestigationRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
