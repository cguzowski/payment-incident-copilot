package com.cguzowski.paymentcopilot.decision;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class HumanDecisionIdentifierGenerator {

    UUID next() {
        return UUID.randomUUID();
    }
}
