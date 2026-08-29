package com.cguzowski.paymentcopilot.knowledge;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class KnowledgeRetrievalIdentifierGenerator {

    UUID next() {
        return UUID.randomUUID();
    }
}
