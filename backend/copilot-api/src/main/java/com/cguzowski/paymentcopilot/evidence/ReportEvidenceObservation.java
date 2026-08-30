package com.cguzowski.paymentcopilot.evidence;

import java.time.Instant;

public record ReportEvidenceObservation(String sourceEventId, Instant observedAt, String errorCode, long count) {}
