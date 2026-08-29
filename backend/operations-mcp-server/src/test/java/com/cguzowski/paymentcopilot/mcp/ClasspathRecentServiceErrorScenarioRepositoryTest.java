package com.cguzowski.paymentcopilot.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ClasspathRecentServiceErrorScenarioRepositoryTest {

    private static final UUID TENANT_ID = UUID.fromString("8b860d80-d17f-4e6b-8c48-af35f26a4d61");
    private final ClasspathRecentServiceErrorScenarioRepository repository =
            new ClasspathRecentServiceErrorScenarioRepository(
                    JsonMapper.builder().findAndAddModules().build());

    @Test
    void loadsAvailableAndAvailableEmptySyntheticScenarios() {
        RecentServiceErrorScenario available =
                repository.find(TENANT_ID, "alert-auth-decline-001").orElseThrow();
        RecentServiceErrorScenario empty =
                repository.find(TENANT_ID, "alert-auth-decline-no-errors").orElseThrow();

        assertThat(available.status()).isEqualTo(EvidenceAvailabilityStatus.AVAILABLE);
        assertThat(available.content().errors()).hasSize(2);
        assertThat(empty.status()).isEqualTo(EvidenceAvailabilityStatus.AVAILABLE);
        assertThat(empty.content().errors()).isEmpty();
    }

    @Test
    void scopesLookupByTenantAndScenarioReference() {
        assertThat(repository.find(UUID.randomUUID(), "alert-auth-decline-001")).isEmpty();
        assertThat(repository.find(TENANT_ID, "unknown-scenario")).isEmpty();
    }

    @Test
    void detectsMalformedFixtureDataWithoutReturningObservations() {
        RecentServiceErrorScenario malformed =
                repository.find(TENANT_ID, "alert-auth-decline-malformed").orElseThrow();

        assertThat(malformed.status()).isEqualTo(EvidenceAvailabilityStatus.MALFORMED);
        assertThat(malformed.statusDetail()).isEqualTo("Synthetic fixture data failed validation.");
        assertThat(malformed.content()).isNull();
    }
}
