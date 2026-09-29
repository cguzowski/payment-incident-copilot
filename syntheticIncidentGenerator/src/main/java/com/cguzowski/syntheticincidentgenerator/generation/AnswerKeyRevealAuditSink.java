package com.cguzowski.syntheticincidentgenerator.generation;

@FunctionalInterface
interface AnswerKeyRevealAuditSink {

    void record(AnswerKeyRevealEvent event);
}
