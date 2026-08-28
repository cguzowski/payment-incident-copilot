package com.cguzowski.paymentcopilot.incident;

final class EvidenceSourceMalformedException extends RuntimeException {

    EvidenceSourceMalformedException() {
        super("The evidence source returned a malformed response.");
    }
}
