package com.cguzowski.paymentcopilot.audit;

final class InvalidAuditTimelineRequestException extends RuntimeException {

    InvalidAuditTimelineRequestException() {
        super("must be a valid UUID");
    }
}
