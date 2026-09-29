package com.gozero.sentiment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Go Zero - Sentiment Service",
        version = "1.0.0",
        description = "Scores reviews from ingestion-service with an in-house lexicon model and tags complaint categories."))
public class SentimentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentimentApplication.class, args);
    }
}
