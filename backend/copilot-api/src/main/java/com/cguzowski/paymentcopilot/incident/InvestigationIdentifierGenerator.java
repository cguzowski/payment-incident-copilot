package com.cguzowski.paymentcopilot.incident;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class InvestigationIdentifierGenerator {

    UUID next() {
        return UUID.randomUUID();
    }
}
