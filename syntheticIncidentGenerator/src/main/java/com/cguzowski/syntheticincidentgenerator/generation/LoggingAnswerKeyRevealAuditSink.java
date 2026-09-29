package com.cguzowski.syntheticincidentgenerator.generation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
final class LoggingAnswerKeyRevealAuditSink implements AnswerKeyRevealAuditSink {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAnswerKeyRevealAuditSink.class);

    @Override
    public void record(AnswerKeyRevealEvent event) {
        LOGGER.info(
                "answer_key_revealed incidentId={} revealedBy={} terminalStatus={} scenarioCode={} oracleVersion={} revealedAt={}",
                event.incidentId(),
                event.revealedBy(),
                event.terminalStatus(),
                event.scenarioCode(),
                event.oracleVersion(),
                event.revealedAt());
    }
}
