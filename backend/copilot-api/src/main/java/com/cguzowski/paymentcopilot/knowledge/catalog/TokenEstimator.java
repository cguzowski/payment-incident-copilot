package com.cguzowski.paymentcopilot.knowledge.catalog;

@FunctionalInterface
interface TokenEstimator {
    int estimate(String text);
}
