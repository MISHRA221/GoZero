package com.gozero.sentiment.analysis;

import java.util.List;

public record SentimentResult(double rawScore, double normalizedScore, SentimentLabel label, List<String> matchedTerms) {
}
