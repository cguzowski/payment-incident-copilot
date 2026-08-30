package com.cguzowski.syntheticincidentgenerator.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertReferenceCodecTest {

    private static final Instant DETECTED_AT = Instant.parse("2026-08-31T09:15:30Z");

    @Test
    void roundTripsOnlyOpaqueScenarioCodeAndDetectionTime() {
        AlertReferenceCodec codec =
                new AlertReferenceCodec(() -> UUID.fromString("12345678-90ab-cdef-1234-567890abcdef"));

        String reference = codec.encode("S203", DETECTED_AT);

        assertThat(reference).isEqualTo("sig-v1-S203-1788167730-1234567890ab");
        assertThat(reference).hasSizeLessThanOrEqualTo(120).doesNotContain("certificate", "ocsp", "root");
        assertThat(new AlertReferenceCodec().decode(reference))
                .contains(new DecodedAlertReference("S203", DETECTED_AT));
    }

    @Test
    void createsAUniqueReferenceForEachGeneratedAlert() {
        java.util.ArrayDeque<UUID> identifiers = new java.util.ArrayDeque<>(java.util.List.of(
                UUID.fromString("12345678-90ab-cdef-1234-567890abcdef"),
                UUID.fromString("abcdef12-3456-7890-abcd-ef1234567890")));
        AlertReferenceCodec codec = new AlertReferenceCodec(identifiers::removeFirst);

        assertThat(codec.encode("S001", DETECTED_AT)).isNotEqualTo(codec.encode("S001", DETECTED_AT));
    }

    @Test
    void rejectsReferencesOutsideTheOwnedVersionedFormat() {
        AlertReferenceCodec codec = new AlertReferenceCodec(UUID::randomUUID);

        assertThat(codec.decode("alert-auth-decline-001")).isEmpty();
        assertThat(codec.decode("sig-v2-S001-1788167730-1234567890ab")).isEmpty();
        assertThat(codec.decode("sig-v1-gateway-timeout-1788167730-1234567890ab"))
                .isEmpty();
        assertThat(codec.decode(null)).isEmpty();
    }
}
