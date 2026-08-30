package com.cguzowski.paymentcopilot.incident;

enum IncidentQueueView {
    ACTIVE,
    COMPLETED;

    static IncidentQueueView parse(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        return switch (value) {
            case "active" -> ACTIVE;
            case "completed" -> COMPLETED;
            default -> throw new InvalidIncidentRequestException("view", "must be active or completed");
        };
    }
}
