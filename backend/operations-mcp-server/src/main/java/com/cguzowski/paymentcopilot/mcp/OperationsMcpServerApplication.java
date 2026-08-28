package com.cguzowski.paymentcopilot.mcp;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.time.Clock;
import java.util.List;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OperationsMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperationsMcpServerApplication.class, args);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    List<SyncToolSpecification> operationsTools(RecentServiceErrorsTool recentServiceErrorsTool) {
        return new SyncMcpToolProvider(List.of(recentServiceErrorsTool)).getToolSpecifications();
    }
}
