package com.cguzowski.paymentcopilot.evidence;

import java.time.Instant;

public record ServiceErrorObservation(String sourceEventId, Instant observedAt, String errorCode, int count) {}
