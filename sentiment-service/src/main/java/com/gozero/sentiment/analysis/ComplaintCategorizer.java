package com.gozero.sentiment.analysis;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Keyword-based complaint tagging: the category with the most keyword hits wins; TASTE_FLAVOR is the fallback. */
public class ComplaintCategorizer {

    private static final Map<ComplaintCategory, List<Pattern>> KEYWORDS = new EnumMap<>(ComplaintCategory.class);

    static {
        KEYWORDS.put(ComplaintCategory.MELTING_TEXTURE, patterns(
                "melt\\w*", "icy", "ice crystals?", "textures?", "grainy", "gritty", "chalky", "watery",
                "gummy", "rubbery", "refrozen", "rock hard", "soggy", "consistency", "mushy", "chewy", "sticky"));
        KEYWORDS.put(ComplaintCategory.SWEETNESS, patterns(
                "sweet\\w*", "sugar\\w*", "aftertaste", "stevia", "erythritol", "sweeteners?", "cloying", "sickly",
                "artificial", "chemical"));
        KEYWORDS.put(ComplaintCategory.PACKAGING_DELIVERY, patterns(
                "packag\\w*", "packed", "packet", "leak\\w*", "lid", "seal\\w*", "dent\\w*", "damag\\w*", "crushed",
                "broken", "broke", "deliver\\w*", "arrived", "courier", "box", "late", "delayed", "cracked"));
        KEYWORDS.put(ComplaintCategory.PRICE_VALUE, patterns(
                "pric\\w*", "expensive", "overpriced", "costly", "value", "money", "worth", "cost\\w*",
                "quantity", "small", "tiny", "shrinkflation", "discount\\w*", "half empty"));
    }

    public ComplaintCategory categorize(String text) {
        if (text == null) {
            return ComplaintCategory.TASTE_FLAVOR;
        }
        String t = text.toLowerCase(Locale.ROOT);
        ComplaintCategory best = ComplaintCategory.TASTE_FLAVOR;
        int bestHits = 0;
        for (Map.Entry<ComplaintCategory, List<Pattern>> e : KEYWORDS.entrySet()) {
            int hits = 0;
            for (Pattern p : e.getValue()) {
                Matcher m = p.matcher(t);
                while (m.find()) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                best = e.getKey();
                bestHits = hits;
            }
        }
        return best;
    }

    private static List<Pattern> patterns(String... regexes) {
        return Arrays.stream(regexes).map(r -> Pattern.compile("\\b" + r + "\\b")).toList();
    }
}
