package com.cguzowski.paymentcopilot.decision;

final class HumanDecisionConflictException extends RuntimeException {

    HumanDecisionConflictException(String message) {
        super(message);
    }
}
