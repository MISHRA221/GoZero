package com.gozero.reconciliation.engine;

import com.gozero.reconciliation.config.ReconciliationProperties;
import com.gozero.reconciliation.domain.AlertType;
import com.gozero.reconciliation.domain.Platform;
import com.gozero.reconciliation.domain.PlatformListing;
import com.gozero.reconciliation.domain.ReconciliationAlert;
import com.gozero.reconciliation.domain.Severity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business rules applied per SKU across platforms:
 * <ul>
 *   <li>PRICE_VARIANCE - a platform's price deviates from the BigBasket reference by more than the threshold</li>
 *   <li>STOCK_MISMATCH - out of stock on some listed platforms while available on others (lost-sales risk)</li>
 *   <li>ASSORTMENT_GAP - SKU not listed on a platform at all</li>
 * </ul>
 */
public class AlertEngine {

    private final ReconciliationProperties props;

    public AlertEngine(ReconciliationProperties props) {
        this.props = props;
    }

    public List<ReconciliationAlert> evaluate(List<PlatformListing> listings, Instant now) {
        Map<Long, List<PlatformListing>> bySku = listings.stream()
                .collect(Collectors.groupingBy(PlatformListing::getProductId, LinkedHashMap::new, Collectors.toList()));
        List<ReconciliationAlert> alerts = new ArrayList<>();
        bySku.values().forEach(group -> evaluateSku(group, now, alerts));
        return alerts;
    }

    private void evaluateSku(List<PlatformListing> group, Instant now, List<ReconciliationAlert> out) {
        PlatformListing reference = group.stream()
                .filter(l -> l.getPlatform() == Platform.BIGBASKET).findFirst().orElse(group.get(0));

        for (PlatformListing l : group) {
            if (!l.isListed() || l.getDeviationPct() == null || l.getPlatform() == Platform.BIGBASKET) {
                continue;
            }
            double dev = l.getDeviationPct();
            if (Math.abs(dev) > props.priceVarianceThresholdPct()) {
                Severity severity = Math.abs(dev) >= props.highSeverityPct() ? Severity.HIGH : Severity.MEDIUM;
                String msg = String.format("%s is Rs %s on %s vs Rs %s BigBasket reference (%+.1f%%)",
                        reference.getProductName(), l.getPrice().toPlainString(), l.getPlatform().displayName(),
                        reference.getPrice().toPlainString(), dev);
                out.add(alert(reference, AlertType.PRICE_VARIANCE, severity, l.getPlatform().displayName(), msg, dev, now));
            }
        }

        List<PlatformListing> listed = group.stream().filter(PlatformListing::isListed).toList();
        List<String> oos = names(listed.stream().filter(l -> !l.isInStock()).toList());
        List<String> available = names(listed.stream().filter(PlatformListing::isInStock).toList());
        if (!oos.isEmpty() && !available.isEmpty()) {
            Severity severity = oos.size() >= 2 ? Severity.HIGH : Severity.MEDIUM;
            String msg = String.format("Out of stock on %s while available on %s - lost-sales risk",
                    String.join(", ", oos), String.join(", ", available));
            out.add(alert(reference, AlertType.STOCK_MISMATCH, severity, String.join(", ", oos), msg,
                    (double) oos.size(), now));
        }

        List<String> unlisted = names(group.stream().filter(l -> !l.isListed()).toList());
        if (!unlisted.isEmpty()) {
            String msg = String.format("Not listed on %s - assortment gap vs other platforms", String.join(", ", unlisted));
            out.add(alert(reference, AlertType.ASSORTMENT_GAP, Severity.LOW, String.join(", ", unlisted), msg,
                    (double) unlisted.size(), now));
        }
    }

    private static List<String> names(List<PlatformListing> listings) {
        return listings.stream().map(l -> l.getPlatform().displayName()).toList();
    }

    private static ReconciliationAlert alert(PlatformListing ref, AlertType type, Severity severity, String platforms,
                                             String message, Double metric, Instant now) {
        ReconciliationAlert a = new ReconciliationAlert();
        a.setProductId(ref.getProductId());
        a.setSku(ref.getSku());
        a.setProductName(ref.getProductName());
        a.setFlavor(ref.getFlavor());
        a.setType(type);
        a.setSeverity(severity);
        a.setPlatforms(platforms);
        a.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        a.setMetricValue(metric);
        a.setCreatedAt(now);
        return a;
    }
}
