package com.gozero.sentiment.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Word/phrase -> integer weight (-3..+3), loaded from a plain-text resource ("phrase weight" per line). */
public final class SentimentLexicon {

    private final Map<String, Integer> weights;
    private final int maxPhraseLength;

    SentimentLexicon(Map<String, Integer> weights) {
        this.weights = Map.copyOf(weights);
        this.maxPhraseLength = weights.keySet().stream().mapToInt(k -> k.split(" ").length).max().orElse(1);
    }

    public static SentimentLexicon fromClasspath(String path) {
        InputStream in = SentimentLexicon.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Lexicon not found on classpath: " + path);
        }
        Map<String, Integer> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                String phrase = String.join(" ", Arrays.copyOf(parts, parts.length - 1)).toLowerCase(Locale.ROOT);
                map.put(phrase, Integer.parseInt(parts[parts.length - 1]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new SentimentLexicon(map);
    }

    public Integer weight(String phrase) {
        return weights.get(phrase);
    }

    public int maxPhraseLength() {
        return maxPhraseLength;
    }

    public int size() {
        return weights.size();
    }
}
