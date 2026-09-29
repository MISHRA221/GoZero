package com.gozero.sentiment.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lexicon scorer: sums matched word/phrase weights (longest phrase first), flips the sign when a negator
 * appears up to 3 tokens earlier in the same clause, then squashes into [-1, 1] (VADER-style normalisation).
 */
public class SentimentScorer {

    private static final Set<String> NEGATORS = Set.of(
            "not", "no", "never", "dont", "doesnt", "didnt", "isnt", "wasnt", "arent", "cant", "couldnt",
            "wont", "hardly", "barely", "without", "nothing", "neither", "nor");
    private static final int NEGATION_WINDOW = 3;
    private static final double ALPHA = 15.0;
    private static final double LABEL_THRESHOLD = 0.2;

    private final SentimentLexicon lexicon;

    public SentimentScorer(SentimentLexicon lexicon) {
        this.lexicon = lexicon;
    }

    public SentimentResult score(String text) {
        double raw = 0;
        List<String> matched = new ArrayList<>();
        for (List<String> tokens : clauses(text)) {
            int i = 0;
            while (i < tokens.size()) {
                Integer weight = null;
                String phrase = null;
                int consumed = 1;
                for (int n = Math.min(lexicon.maxPhraseLength(), tokens.size() - i); n >= 1; n--) {
                    String candidate = String.join(" ", tokens.subList(i, i + n));
                    Integer w = lexicon.weight(candidate);
                    if (w != null) {
                        weight = w;
                        phrase = candidate;
                        consumed = n;
                        break;
                    }
                }
                if (weight != null) {
                    boolean negated = isNegated(tokens, i);
                    int applied = negated ? -weight : weight;
                    raw += applied;
                    matched.add((negated ? "not " : "") + phrase + "(" + (applied > 0 ? "+" : "") + applied + ")");
                }
                i += consumed;
            }
        }
        double normalized = raw == 0 ? 0 : raw / Math.sqrt(raw * raw + ALPHA);
        normalized = Math.round(normalized * 1000) / 1000.0;
        SentimentLabel label = normalized >= LABEL_THRESHOLD ? SentimentLabel.POSITIVE
                : normalized <= -LABEL_THRESHOLD ? SentimentLabel.NEGATIVE : SentimentLabel.NEUTRAL;
        return new SentimentResult(raw, normalized, label, matched);
    }

    static List<List<String>> clauses(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT).replace('\u2019', '\'').replace("'", "");
        List<List<String>> out = new ArrayList<>();
        for (String clause : normalized.split("[.,!?;:]+|\\bbut\\b")) {
            List<String> tokens = Arrays.stream(clause.split("[^a-z]+")).filter(t -> !t.isEmpty()).toList();
            if (!tokens.isEmpty()) {
                out.add(tokens);
            }
        }
        return out;
    }

    private static boolean isNegated(List<String> tokens, int index) {
        for (int j = Math.max(0, index - NEGATION_WINDOW); j < index; j++) {
            if (NEGATORS.contains(tokens.get(j))) {
                return true;
            }
        }
        return false;
    }
}
