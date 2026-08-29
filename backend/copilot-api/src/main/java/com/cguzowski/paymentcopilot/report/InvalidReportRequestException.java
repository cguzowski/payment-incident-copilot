package com.cguzowski.paymentcopilot.report;

final class InvalidReportRequestException extends RuntimeException {

    private final String field;

    InvalidReportRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    String field() {
        return field;
    }
}
