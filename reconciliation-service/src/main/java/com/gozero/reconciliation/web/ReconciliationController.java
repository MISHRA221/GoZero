package com.gozero.reconciliation.web;

import com.gozero.reconciliation.domain.AlertType;
import com.gozero.reconciliation.domain.Severity;
import com.gozero.reconciliation.service.ReconciliationService;
import com.gozero.reconciliation.service.RunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reconciliation")
@Tag(name = "Reconciliation (SIMULATED)", description = "Cross-platform price/stock checks on simulated quick-commerce data")
public class ReconciliationController {

    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    @GetMapping("/alerts")
    @Operation(summary = "Active alerts (price variance, stock mismatch, assortment gap) - SIMULATED")
    public ApiEnvelope<List<AlertDto>> alerts(@RequestParam(required = false) Severity severity,
                                              @RequestParam(required = false) AlertType type) {
        return service.alerts(severity, type);
    }

    @GetMapping("/by-sku/{id}")
    @Operation(summary = "All platform listings and alerts for one product id (from ingestion-service) - SIMULATED")
    public ApiEnvelope<SkuReconciliationDto> bySku(@PathVariable Long id) {
        return service.bySku(id);
    }

    @GetMapping("/listings")
    @Operation(summary = "Every simulated platform listing (used by the dashboard's price comparison chart)")
    public ApiEnvelope<List<ListingDto>> listings() {
        return service.listings();
    }

    @PostMapping("/run")
    @Operation(summary = "Re-pull SKUs from ingestion-service, regenerate simulated listings and re-evaluate alerts")
    public RunResult run() {
        return service.run();
    }
}
