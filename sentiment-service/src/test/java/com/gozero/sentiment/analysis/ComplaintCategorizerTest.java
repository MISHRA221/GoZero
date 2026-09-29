package com.gozero.sentiment.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplaintCategorizerTest {

    private final ComplaintCategorizer categorizer = new ComplaintCategorizer();

    @Test
    void tagsEachCategory() {
        assertThat(categorizer.categorize("Icy and watery, it melted on the way")).isEqualTo(ComplaintCategory.MELTING_TEXTURE);
        assertThat(categorizer.categorize("Too sweet with an artificial aftertaste")).isEqualTo(ComplaintCategory.SWEETNESS);
        assertThat(categorizer.categorize("Lid was broken and the packaging leaked on delivery")).isEqualTo(ComplaintCategory.PACKAGING_DELIVERY);
        assertThat(categorizer.categorize("Overpriced, not worth the money")).isEqualTo(ComplaintCategory.PRICE_VALUE);
    }

    @Test
    void fallsBackToTasteFlavor() {
        assertThat(categorizer.categorize("Mint tastes like toothpaste")).isEqualTo(ComplaintCategory.TASTE_FLAVOR);
    }

    @Test
    void parsesCategoryParams() {
        assertThat(ComplaintCategory.fromParam("melting/texture")).isEqualTo(ComplaintCategory.MELTING_TEXTURE);
        assertThat(ComplaintCategory.fromParam("price")).isEqualTo(ComplaintCategory.PRICE_VALUE);
        assertThatThrownBy(() -> ComplaintCategory.fromParam("xyz")).isInstanceOf(IllegalArgumentException.class);
    }
}
