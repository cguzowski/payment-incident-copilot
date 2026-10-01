package com.cguzowski.paymentcopilot.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

class OperationsMcpInfoContributorTest {
    @Test
    void reportsTheEffectiveProviderEndpoint() {
        Info.Builder info = new Info.Builder();
        new OperationsMcpInfoContributor("http://localhost:8082").contribute(info);
        assertThat(info.build().getDetails())
                .containsEntry("operationsMcp", Map.of("baseUrl", "http://localhost:8082"));
    }

    @Test
    void excludesCredentialsQueryAndFragment() {
        Info.Builder info = new Info.Builder();
        new OperationsMcpInfoContributor("https://user:secret@example.test:8443/provider/?token=secret#private")
                .contribute(info);
        assertThat(info.build().getDetails())
                .containsEntry("operationsMcp", Map.of("baseUrl", "https://example.test:8443/provider"));
    }
}
