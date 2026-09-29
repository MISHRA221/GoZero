package com.gozero.ingestion.web;

import com.gozero.ingestion.repository.ProductRepository;
import com.gozero.ingestion.repository.ReviewRepository;
import com.gozero.ingestion.service.IngestionService;
import com.gozero.ingestion.service.RefreshResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Go Zero catalogue as ingested from BigBasket (or the flagged seed dataset)")
public class ProductController {

    private final ProductRepository products;
    private final ReviewRepository reviews;
    private final IngestionService ingestionService;

    public ProductController(ProductRepository products, ReviewRepository reviews, IngestionService ingestionService) {
        this.products = products;
        this.reviews = reviews;
        this.ingestionService = ingestionService;
    }

    @GetMapping
    @Operation(summary = "List all ingested Go Zero products")
    public ApiEnvelope<List<ProductDto>> list() {
        List<ProductDto> data = products.findAllByOrderByFlavorAscNameAsc().stream().map(ProductDto::from).toList();
        RefreshResult last = ingestionService.lastRefresh();
        boolean live = last != null && last.isLiveData();
        String source = last == null ? "PENDING" : last.dataSource();
        String note = live
                ? "Live scrape of bigbasket.com/pb/go-zero/ at " + last.refreshedAt()
                : "SEED DATASET - illustrative values bundled with the app, NOT live Go Zero data."
                + (last != null && last.fallbackReason() != null ? " Reason: " + last.fallbackReason() : "");
        return new ApiEnvelope<>(live, source, note, Instant.now(), data.size(), data);
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Reviews for one product (each review carries its own isLiveData flag)")
    public ApiEnvelope<List<ReviewDto>> reviews(@PathVariable Long id) {
        if (!products.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found");
        }
        List<ReviewDto> data = reviews.findByProductIdOrderByIdAsc(id).stream().map(ReviewDto::from).toList();
        long liveCount = data.stream().filter(ReviewDto::isLiveData).count();
        boolean allLive = !data.isEmpty() && liveCount == data.size();
        String source = data.isEmpty() ? "NONE" : allLive ? IngestionService.SOURCE_LIVE
                : liveCount == 0 ? IngestionService.SOURCE_SEED : "MIXED";
        String note = allLive ? "Reviews scraped from BigBasket product page"
                : "Contains seed reviews (illustrative, not real customer reviews) - see per-review isLiveData";
        return new ApiEnvelope<>(allLive, source, note, Instant.now(), data.size(), data);
    }
}
