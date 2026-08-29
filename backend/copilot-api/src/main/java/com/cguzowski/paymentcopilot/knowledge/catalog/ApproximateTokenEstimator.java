package com.cguzowski.paymentcopilot.knowledge.catalog;

final class ApproximateTokenEstimator implements TokenEstimator {

    @Override
    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 4.0d));
    }
}
