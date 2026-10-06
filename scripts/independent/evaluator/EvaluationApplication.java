package com.cguzowski.syntheticincidentgenerator;

import com.cguzowski.syntheticincidentgenerator.comparison.ComparisonProperties;
import com.cguzowski.syntheticincidentgenerator.config.GeneratorProperties;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({GeneratorProperties.class, ComparisonProperties.class})
public class EvaluationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvaluationApplication.class, args);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
