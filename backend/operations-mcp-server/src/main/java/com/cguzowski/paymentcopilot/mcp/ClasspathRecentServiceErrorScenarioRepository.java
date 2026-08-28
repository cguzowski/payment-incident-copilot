package com.cguzowski.paymentcopilot.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
class ClasspathRecentServiceErrorScenarioRepository implements RecentServiceErrorScenarioRepository {

    private static final String FIXTURE_PATH = "fixtures/recent-service-errors.json";
    private static final String MALFORMED_DETAIL = "Synthetic fixture data failed validation.";

    private final Map<ScenarioKey, RecentServiceErrorScenario> scenarios;

    ClasspathRecentServiceErrorScenarioRepository(JsonMapper jsonMapper) {
        this.scenarios = load(jsonMapper);
    }

    @Override
    public Optional<RecentServiceErrorScenario> find(UUID tenantId, String scenarioReference) {
        return Optional.ofNullable(scenarios.get(new ScenarioKey(tenantId, scenarioReference)));
    }

    private static Map<ScenarioKey, RecentServiceErrorScenario> load(JsonMapper jsonMapper) {
        try (InputStream input = new ClassPathResource(FIXTURE_PATH).getInputStream()) {
            FixtureDocument document = jsonMapper.readValue(input, FixtureDocument.class);
            if (document == null || document.scenarios() == null) {
                throw new IllegalStateException("Synthetic service-error fixture has no scenarios.");
            }
            return document.scenarios().stream().collect(Collectors.toUnmodifiableMap(
                    ClasspathRecentServiceErrorScenarioRepository::key,
                    ClasspathRecentServiceErrorScenarioRepository::scenario,
                    rejectingDuplicates()));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load synthetic service-error fixtures.", exception);
        }
    }

    private static BinaryOperator<RecentServiceErrorScenario> rejectingDuplicates() {
        return (first, second) -> {
            throw new IllegalStateException("Synthetic service-error fixture contains a duplicate scenario.");
        };
    }

    private static ScenarioKey key(FixtureScenario fixture) {
        if (fixture.tenantId() == null
                || fixture.scenarioReference() == null
                || fixture.scenarioReference().isBlank()
                || fixture.scenarioReference().length() > 120) {
            throw new IllegalStateException("Synthetic service-error fixture has an invalid key.");
        }
        return new ScenarioKey(fixture.tenantId(), fixture.scenarioReference());
    }

    private static RecentServiceErrorScenario scenario(FixtureScenario fixture) {
        try {
            EvidenceAvailabilityStatus status = EvidenceAvailabilityStatus.valueOf(fixture.status());
            RecentServiceErrorsContent content = fixture.content() == null ? null : content(fixture.content());
            if ((status == EvidenceAvailabilityStatus.AVAILABLE || status == EvidenceAvailabilityStatus.PARTIAL)
                    && content == null) {
                return malformed();
            }
            if (status != EvidenceAvailabilityStatus.AVAILABLE
                    && status != EvidenceAvailabilityStatus.PARTIAL
                    && content != null) {
                return malformed();
            }
            if (fixture.statusDetail() != null && fixture.statusDetail().length() > 500) {
                return malformed();
            }
            return new RecentServiceErrorScenario(status, fixture.statusDetail(), content);
        } catch (RuntimeException exception) {
            return malformed();
        }
    }

    private static RecentServiceErrorsContent content(FixtureContent fixture) {
        if (fixture.serviceName() == null
                || fixture.serviceName().isBlank()
                || fixture.observedFrom() == null
                || fixture.observedTo() == null
                || fixture.observedFrom().isAfter(fixture.observedTo())
                || fixture.errors() == null) {
            throw new IllegalArgumentException("Invalid service-error content.");
        }
        List<RecentServiceErrorObservation> observations = fixture.errors().stream()
                .map(ClasspathRecentServiceErrorScenarioRepository::observation)
                .toList();
        return new RecentServiceErrorsContent(
                fixture.serviceName(),
                fixture.observedFrom(),
                fixture.observedTo(),
                observations);
    }

    private static RecentServiceErrorObservation observation(FixtureObservation fixture) {
        if (fixture.sourceEventId() == null
                || fixture.sourceEventId().isBlank()
                || fixture.observedAt() == null
                || fixture.errorCode() == null
                || fixture.errorCode().isBlank()
                || fixture.count() <= 0) {
            throw new IllegalArgumentException("Invalid service-error observation.");
        }
        return new RecentServiceErrorObservation(
                fixture.sourceEventId(),
                fixture.observedAt(),
                fixture.errorCode(),
                fixture.count());
    }

    private static RecentServiceErrorScenario malformed() {
        return new RecentServiceErrorScenario(
                EvidenceAvailabilityStatus.MALFORMED,
                MALFORMED_DETAIL,
                null);
    }

    private record ScenarioKey(UUID tenantId, String scenarioReference) {
    }

    private record FixtureDocument(List<FixtureScenario> scenarios) {
    }

    private record FixtureScenario(
            UUID tenantId,
            String scenarioReference,
            String status,
            String statusDetail,
            FixtureContent content) {
    }

    private record FixtureContent(
            String serviceName,
            Instant observedFrom,
            Instant observedTo,
            List<FixtureObservation> errors) {
    }

    private record FixtureObservation(
            String sourceEventId,
            Instant observedAt,
            String errorCode,
            int count) {
    }
}
