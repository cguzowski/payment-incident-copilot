package com.cguzowski.paymentcopilot.knowledge;

@FunctionalInterface
interface TokenEstimator {
    int estimate(String text);
}
