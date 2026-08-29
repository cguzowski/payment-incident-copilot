package com.cguzowski.paymentcopilot.evidence;

final class EvidenceSourceMalformedException extends RuntimeException {

    EvidenceSourceMalformedException() {
        super("The evidence source returned a malformed response.");
    }
}
