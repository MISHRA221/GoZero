package com.gozero.reconciliation.web;

import java.util.List;

public record SkuReconciliationDto(
        Long productId,
        String sku,
        String productName,
        String flavor,
        String packSize,
        List<ListingDto> listings,
        List<AlertDto> alerts) {
}
