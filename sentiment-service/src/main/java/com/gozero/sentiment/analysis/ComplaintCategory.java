package com.gozero.sentiment.analysis;

import java.util.Arrays;
import java.util.Locale;

public enum ComplaintCategory {
    MELTING_TEXTURE("Melting / Texture"),
    SWEETNESS("Sweetness"),
    PACKAGING_DELIVERY("Packaging / Delivery"),
    PRICE_VALUE("Price / Value"),
    TASTE_FLAVOR("Taste / Flavor");

    private final String label;

    ComplaintCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Accepts enum names, labels ("melting/texture") or partial names ("price", "texture"). */
    public static ComplaintCategory fromParam(String value) {
        String norm = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]+", "_").replaceAll("^_|_$", "");
        return Arrays.stream(values())
                .filter(c -> c.name().equals(norm) || (norm.length() >= 4 && c.name().contains(norm)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown complaint category '" + value
                        + "'. Valid: " + Arrays.toString(values())));
    }
}
