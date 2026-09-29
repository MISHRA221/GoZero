package com.gozero.dashboard.web;

import com.gozero.dashboard.service.DashboardService;
import com.gozero.dashboard.service.DashboardView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard gateway", description = "Aggregated view over ingestion, sentiment and reconciliation services")
public class DashboardApiController {

    private final DashboardService service;

    public DashboardApiController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Everything the dashboard renders, as JSON (with provenance flags)")
    public DashboardView summary() {
        return service.build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Re-run the pipeline: re-scrape -> re-score sentiment -> re-simulate reconciliation")
    public List<String> refresh() {
        return service.refreshPipeline();
    }
}
