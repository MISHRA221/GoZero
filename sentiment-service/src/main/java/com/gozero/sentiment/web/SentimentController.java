package com.gozero.sentiment.web;

import com.gozero.sentiment.analysis.ComplaintCategory;
import com.gozero.sentiment.service.RecomputeResult;
import com.gozero.sentiment.service.SentimentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@Tag(name = "Sentiment", description = "Lexicon-based sentiment and complaint categories per flavor")
public class SentimentController {

    private final SentimentAnalysisService service;

    public SentimentController(SentimentAnalysisService service) {
        this.service = service;
    }

    @GetMapping("/by-flavor")
    @Operation(summary = "Sentiment breakdown per flavor, sorted by negative review volume")
    public ApiEnvelope<List<FlavorSentimentDto>> byFlavor() {
        return service.byFlavor();
    }

    @GetMapping("/complaints")
    @Operation(summary = "Negative reviews, optionally filtered by complaint category and/or flavor")
    public ApiEnvelope<List<ComplaintDto>> complaints(
            @Parameter(description = "MELTING_TEXTURE, SWEETNESS, PACKAGING_DELIVERY, PRICE_VALUE, TASTE_FLAVOR "
                    + "(partial names like 'price' also work)")
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String flavor) {
        ComplaintCategory c = (category == null || category.isBlank()) ? null : ComplaintCategory.fromParam(category);
        return service.complaints(c, flavor);
    }

    @GetMapping("/categories")
    @Operation(summary = "Complaint categories understood by the tagger")
    public Map<String, String> categories() {
        Map<String, String> out = new LinkedHashMap<>();
        Arrays.stream(ComplaintCategory.values()).forEach(c -> out.put(c.name(), c.label()));
        return out;
    }

    @PostMapping("/recompute")
    @Operation(summary = "Re-pull reviews from ingestion-service and re-score everything")
    public RecomputeResult recompute() {
        return service.recompute();
    }
}
