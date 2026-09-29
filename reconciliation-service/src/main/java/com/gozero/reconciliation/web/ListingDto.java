package com.gozero.reconciliation.web;

import com.gozero.reconciliation.domain.PlatformListing;

import java.math.BigDecimal;

public record ListingDto(
        Long productId,
        String sku,
        String productName,
        String flavor,
        String packSize,
        String platform,
        String platformName,
        BigDecimal price,
        BigDecimal mrp,
        boolean inStock,
        boolean listed,
        Double deviationPct,
        String priceSource) {

    public static ListingDto from(PlatformListing l) {
        return new ListingDto(l.getProductId(), l.getSku(), l.getProductName(), l.getFlavor(), l.getPackSize(),
                l.getPlatform().name(), l.getPlatform().displayName(), l.getPrice(), l.getMrp(), l.isInStock(),
                l.isListed(), l.getDeviationPct(), l.getPriceSource());
    }
}
