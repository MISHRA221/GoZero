package com.gozero.sentiment.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SentimentScorerTest {

    private static SentimentScorer scorer;

    @BeforeAll
    static void setUp() {
        SentimentLexicon lexicon = SentimentLexicon.fromClasspath("lexicon/sentiment-lexicon.txt");
        assertThat(lexicon.size()).isGreaterThanOrEqualTo(200);
        scorer = new SentimentScorer(lexicon);
    }

    @Test
    void positiveReview() {
        SentimentResult r = scorer.score("Rich, creamy and super chocolaty. My favourite late night treat!");
        assertThat(r.label()).isEqualTo(SentimentLabel.POSITIVE);
        assertThat(r.normalizedScore()).isPositive();
    }

    @Test
    void negativeReviewWithPhrases() {
        SentimentResult r = scorer.score("Arrived half melted with ice crystals and a grainy texture. Disappointing.");
        assertThat(r.label()).isEqualTo(SentimentLabel.NEGATIVE);
        assertThat(r.matchedTerms()).anyMatch(t -> t.startsWith("ice crystals"));
    }

    @Test
    void negationFlipsPolarityWithinClause() {
        assertThat(scorer.score("Not good at all").label()).isEqualTo(SentimentLabel.NEGATIVE);
        assertThat(scorer.score("Not too sweet, just right").label()).isEqualTo(SentimentLabel.POSITIVE);
    }

    @Test
    void negationDoesNotCrossClauseBoundary() {
        assertThat(scorer.score("No complaints, amazing taste").label()).isEqualTo(SentimentLabel.POSITIVE);
    }

    @Test
    void emptyTextIsNeutral() {
        assertThat(scorer.score("").label()).isEqualTo(SentimentLabel.NEUTRAL);
        assertThat(scorer.score(null).normalizedScore()).isZero();
    }
}
