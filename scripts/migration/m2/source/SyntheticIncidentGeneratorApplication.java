package com.cguzowski.syntheticincidentgenerator;

import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import com.cguzowski.syntheticincidentgenerator.mcp.RecentServiceErrorsTool;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.time.Clock;
import java.util.List;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(GeneratorProperties.class)
public class SyntheticIncidentGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyntheticIncidentGeneratorApplication.class, args);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    List<SyncToolSpecification> evidenceTools(RecentServiceErrorsTool recentServiceErrorsTool) {
        return new SyncMcpToolProvider(List.of(recentServiceErrorsTool)).getToolSpecifications();
    }
}
