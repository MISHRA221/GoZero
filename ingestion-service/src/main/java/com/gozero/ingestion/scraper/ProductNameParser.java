package com.gozero.ingestion.scraper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives flavor and pack size from marketplace product titles,
 * e.g. "Go Zero Belgian Chocolate Ice Cream - Low Calorie, 500 ml".
 */
public final class ProductNameParser {

    private static final Pattern PACK = Pattern.compile(
            "(?i)(\\d+\\s*x\\s*\\d+(?:\\.\\d+)?\\s*(?:ml|g|l)\\b|\\d+(?:\\.\\d+)?\\s*(?:ml|l|g|kg|pcs|pc)\\b)");

    private static final Pattern NOISE = Pattern.compile(
            "(?i)\\b(go\\s*zero|ice\\s*cream|frozen\\s*dessert|low\\s*calorie|low\\s*sugar|no\\s*added\\s*sugar"
                    + "|sugar\\s*free|high\\s*protein|guilt[\\s-]*free)\\b");

    private ProductNameParser() {
    }

    public static String packSize(String name) {
        if (name == null) {
            return null;
        }
        Matcher m = PACK.matcher(name);
        return m.find() ? m.group(1).replaceAll("\\s+", " ").trim() : null;
    }

    public static String flavor(String name) {
        if (name == null) {
            return "Unknown";
        }
        String s = PACK.matcher(name).replaceAll(" ");
        s = NOISE.matcher(s).replaceAll(" ");
        for (String part : s.split("[-,|(/)]")) {
            String candidate = part.replaceAll("\\s+", " ").trim();
            if (candidate.length() > 1) {
                return candidate;
            }
        }
        return "Assorted";
    }
}
