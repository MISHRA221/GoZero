package com.gozero.sentiment.service;

import com.gozero.sentiment.analysis.ComplaintCategorizer;
import com.gozero.sentiment.analysis.ComplaintCategory;
import com.gozero.sentiment.analysis.SentimentLabel;
import com.gozero.sentiment.analysis.SentimentResult;
import com.gozero.sentiment.analysis.SentimentScorer;
import com.gozero.sentiment.client.IngestionClient;
import com.gozero.sentiment.client.IngestionDtos.Product;
import com.gozero.sentiment.client.IngestionDtos.Review;
import com.gozero.sentiment.domain.ReviewSentiment;
import com.gozero.sentiment.domain.ReviewSentimentRepository;
import com.gozero.sentiment.web.ApiEnvelope;
import com.gozero.sentiment.web.ComplaintDto;
import com.gozero.sentiment.web.FlavorSentimentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class SentimentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisService.class);
    private static final String NOTE = "Sentiment computed in-house by a lexicon-based scorer over reviews supplied by "
            + "ingestion-service. Check isLiveData: seed reviews are illustrative, not real customer reviews.";

    private final IngestionClient ingestion;
    private final SentimentScorer scorer;
    private final ComplaintCategorizer categorizer;
    private final ReviewSentimentRepository repository;
    private final TransactionTemplate tx;

    public SentimentAnalysisService(IngestionClient ingestion, SentimentScorer scorer, ComplaintCategorizer categorizer,
                                    ReviewSentimentRepository repository, TransactionTemplate tx) {
        this.ingestion = ingestion;
        this.scorer = scorer;
        this.categorizer = categorizer;
        this.repository = repository;
        this.tx = tx;
    }

    public synchronized RecomputeResult recompute() {
        List<Product> products = ingestion.products();
        Instant now = Instant.now();
        List<ReviewSentiment> rows = new ArrayList<>();
        for (Product p : products) {
            for (Review r : ingestion.reviews(p.id())) {
                rows.add(analyze(p, r, now));
            }
        }
        tx.executeWithoutResult(status -> {
            repository.deleteAllInBatch();
            repository.saveAll(rows);
        });
        int negative = (int) rows.stream().filter(r -> SentimentLabel.NEGATIVE.name().equals(r.getLabel())).count();
        boolean live = !rows.isEmpty() && rows.stream().allMatch(ReviewSentiment::isLiveData);
        log.info("Analyzed {} reviews across {} products ({} negative)", rows.size(), products.size(), negative);
        return new RecomputeResult(products.size(), rows.size(), negative, live, now);
    }

    public ApiEnvelope<List<FlavorSentimentDto>> byFlavor() {
        List<ReviewSentiment> all = analyzedRows();
        Map<String, List<ReviewSentiment>> grouped = all.stream()
                .collect(Collectors.groupingBy(r -> Objects.requireNonNullElse(r.getFlavor(), "Unknown"),
                        TreeMap::new, Collectors.toList()));

        List<FlavorSentimentDto> data = grouped.entrySet().stream()
                .map(e -> toFlavorDto(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(FlavorSentimentDto::negative).reversed()
                        .thenComparing(Comparator.comparingDouble(FlavorSentimentDto::negativePct).reversed())
                        .thenComparing(FlavorSentimentDto::flavor))
                .toList();
        return envelope(all, data, data.size());
    }

    public ApiEnvelope<List<ComplaintDto>> complaints(ComplaintCategory category, String flavor) {
        analyzedRows();
        List<ReviewSentiment> negatives = repository.findByLabelOrderByNormalizedScoreAsc(SentimentLabel.NEGATIVE.name())
                .stream()
                .filter(r -> category == null || category.name().equals(r.getCategory()))
                .filter(r -> flavor == null || flavor.equalsIgnoreCase(r.getFlavor()))
                .toList();
        List<ComplaintDto> data = negatives.stream().map(ComplaintDto::from).toList();
        return envelope(negatives, data, data.size());
    }

    /** Lazily analyses on first read so the service works regardless of startup order. */
    private List<ReviewSentiment> analyzedRows() {
        if (repository.count() == 0) {
            recompute();
        }
        return repository.findAll();
    }

    private FlavorSentimentDto toFlavorDto(String flavor, List<ReviewSentiment> rows) {
        Map<String, Long> byLabel = rows.stream()
                .collect(Collectors.groupingBy(ReviewSentiment::getLabel, Collectors.counting()));
        int pos = byLabel.getOrDefault(SentimentLabel.POSITIVE.name(), 0L).intValue();
        int neu = byLabel.getOrDefault(SentimentLabel.NEUTRAL.name(), 0L).intValue();
        int neg = byLabel.getOrDefault(SentimentLabel.NEGATIVE.name(), 0L).intValue();
        double avg = rows.stream().mapToDouble(ReviewSentiment::getNormalizedScore).average().orElse(0);
        String topComplaint = rows.stream()
                .filter(r -> SentimentLabel.NEGATIVE.name().equals(r.getLabel()))
                .collect(Collectors.groupingBy(ReviewSentiment::getCategory, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> ComplaintCategory.valueOf(e.getKey()).label())
                .orElse(null);
        return new FlavorSentimentDto(flavor, rows.size(), pos, neu, neg, round1(neg * 100.0 / rows.size()),
                Math.round(avg * 1000) / 1000.0, topComplaint, rows.stream().allMatch(ReviewSentiment::isLiveData));
    }

    private ReviewSentiment analyze(Product p, Review r, Instant now) {
        SentimentResult result = scorer.score(r.text());
        ReviewSentiment row = new ReviewSentiment();
        row.setReviewId(r.id());
        row.setProductId(p.id());
        row.setProductName(p.name());
        row.setFlavor(p.flavor());
        row.setReviewText(r.text());
        row.setStarRating(r.rating());
        row.setRawScore(result.rawScore());
        row.setNormalizedScore(result.normalizedScore());
        row.setLabel(result.label().name());
        row.setCategory(categorizer.categorize(r.text()).name());
        String terms = String.join(", ", result.matchedTerms());
        row.setMatchedTerms(terms.length() > 1000 ? terms.substring(0, 1000) : terms);
        row.setLiveData(r.isLiveData());
        row.setAnalyzedAt(now);
        return row;
    }

    private static <T> ApiEnvelope<T> envelope(List<ReviewSentiment> rows, T data, int count) {
        long live = rows.stream().filter(ReviewSentiment::isLiveData).count();
        boolean allLive = !rows.isEmpty() && live == rows.size();
        String source = rows.isEmpty() ? "NONE" : allLive ? "BIGBASKET_LIVE_SCRAPE" : live == 0 ? "SEED_DATASET" : "MIXED";
        return new ApiEnvelope<>(allLive, source, NOTE, Instant.now(), count, data);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
