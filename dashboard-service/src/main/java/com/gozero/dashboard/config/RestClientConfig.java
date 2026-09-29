package com.gozero.dashboard.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder, ServiceProperties props) {
        return builder
                .setConnectTimeout(Duration.ofMillis(props.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(props.readTimeoutMs()))
                .build();
    }
}
