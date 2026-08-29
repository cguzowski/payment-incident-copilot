package com.cguzowski.paymentcopilot.requestcontext;

public final class InvalidSyntheticRequestContextException extends RuntimeException {

    private final String field;

    InvalidSyntheticRequestContextException(String field, String message) {
        super(message);
        this.field = field;
    }

    String field() {
        return field;
    }
}
