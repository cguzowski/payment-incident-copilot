package com.cguzowski.paymentcopilot.evidence;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class EvidenceIdentifierGenerator {

    UUID next() {
        return UUID.randomUUID();
    }
}
