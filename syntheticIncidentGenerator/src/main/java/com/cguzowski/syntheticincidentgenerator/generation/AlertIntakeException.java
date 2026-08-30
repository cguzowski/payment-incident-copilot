package com.cguzowski.syntheticincidentgenerator.generation;

public class AlertIntakeException extends RuntimeException {

    public AlertIntakeException(String message) {
        super(message);
    }

    public AlertIntakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
