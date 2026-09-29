package com.gozero.ingestion.web;

import com.gozero.ingestion.service.IngestionService;
import com.gozero.ingestion.service.RefreshResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ingest")
@Tag(name = "Ingestion", description = "Trigger and inspect scraping runs")
public class IngestController {

    private final IngestionService ingestionService;

    public IngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Re-scrape BigBasket (robots.txt-aware, rate limited); falls back to seed data if blocked")
    public RefreshResult refresh() {
        return ingestionService.refresh();
    }

    @GetMapping("/status")
    @Operation(summary = "Result of the most recent ingestion run")
    public RefreshResult status() {
        RefreshResult last = ingestionService.lastRefresh();
        if (last == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Initial ingestion still running");
        }
        return last;
    }
}
