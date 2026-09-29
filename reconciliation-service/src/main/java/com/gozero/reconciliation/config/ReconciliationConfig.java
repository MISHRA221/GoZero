package com.gozero.reconciliation.config;

import com.gozero.reconciliation.engine.AlertEngine;
import com.gozero.reconciliation.simulation.PlatformSimulator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ReconciliationConfig {

    @Bean
    PlatformSimulator platformSimulator(ReconciliationProperties props) {
        return new PlatformSimulator(props);
    }

    @Bean
    AlertEngine alertEngine(ReconciliationProperties props) {
        return new AlertEngine(props);
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(20)).build();
    }
}
