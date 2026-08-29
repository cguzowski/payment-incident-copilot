package com.cguzowski.paymentcopilot.mcp;

import java.time.Instant;

public record RecentServiceErrorObservation(String sourceEventId, Instant observedAt, String errorCode, int count) {}
