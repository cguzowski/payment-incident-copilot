package com.cguzowski.paymentcopilot.report;

record ReportModelSettings(String version, int contextTokens, boolean stream, boolean truncate, boolean shift) {}
