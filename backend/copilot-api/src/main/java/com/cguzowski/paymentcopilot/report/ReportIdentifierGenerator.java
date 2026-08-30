package com.cguzowski.paymentcopilot.report;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ReportIdentifierGenerator {

    UUID next() {
        return UUID.randomUUID();
    }
}
