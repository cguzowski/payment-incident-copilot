package com.cguzowski.syntheticincidentgenerator.scenario;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AlertReferenceCodec {

    private static final Pattern REFERENCE_PATTERN =
            Pattern.compile("^sig-v1-(S[0-9]{3})-([0-9]{1,11})-([0-9a-f]{12})$");

    private final Supplier<UUID> identifiers;

    public AlertReferenceCodec() {
        this(UUID::randomUUID);
    }

    public AlertReferenceCodec(Supplier<UUID> identifiers) {
        this.identifiers = identifiers;
    }

    public String encode(String scenarioCode, Instant detectedAt) {
        if (scenarioCode == null || !scenarioCode.matches("S[0-9]{3}") || detectedAt == null) {
            throw new IllegalArgumentException("Scenario code and detection time are required.");
        }
        String token = identifiers.get().toString().replace("-", "").substring(0, 12);
        return "sig-v1-" + scenarioCode + "-" + detectedAt.getEpochSecond() + "-" + token;
    }

    public Optional<DecodedAlertReference> decode(String reference) {
        if (reference == null) {
            return Optional.empty();
        }
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DecodedAlertReference(
                    matcher.group(1), Instant.ofEpochSecond(Long.parseLong(matcher.group(2)))));
        } catch (DateTimeException | NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
