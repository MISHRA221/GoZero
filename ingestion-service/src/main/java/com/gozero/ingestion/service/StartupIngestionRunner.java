package com.gozero.ingestion.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Runs before the readiness probe flips to ACCEPTING_TRAFFIC, so downstream services never see an empty catalogue. */
@Component
public class StartupIngestionRunner implements ApplicationRunner {

    private final IngestionService ingestionService;

    public StartupIngestionRunner(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ingestionService.refresh();
    }
}
