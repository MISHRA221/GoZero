package com.gozero.sentiment.config;

import com.gozero.sentiment.analysis.ComplaintCategorizer;
import com.gozero.sentiment.analysis.SentimentLexicon;
import com.gozero.sentiment.analysis.SentimentScorer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class SentimentConfig {

    @Bean
    SentimentLexicon sentimentLexicon(@Value("${gozero.sentiment.lexicon-path}") String path) {
        return SentimentLexicon.fromClasspath(path);
    }

    @Bean
    SentimentScorer sentimentScorer(SentimentLexicon lexicon) {
        return new SentimentScorer(lexicon);
    }

    @Bean
    ComplaintCategorizer complaintCategorizer() {
        return new ComplaintCategorizer();
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(20)).build();
    }
}
