package com.gozero.ingestion.scraper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductNameParserTest {

    @Test
    void extractsFlavorAndPackSizeFromMarketplaceTitle() {
        String title = "Go Zero Belgian Chocolate Ice Cream - Low Calorie, 500 ml";
        assertThat(ProductNameParser.flavor(title)).isEqualTo("Belgian Chocolate");
        assertThat(ProductNameParser.packSize(title)).isEqualTo("500 ml");
    }

    @Test
    void handlesMultipacksAndAmpersands() {
        assertThat(ProductNameParser.packSize("Go Zero Choco Almond Stick 4 x 70 ml")).isEqualTo("4 x 70 ml");
        assertThat(ProductNameParser.flavor("GO ZERO Cookies & Cream Frozen Dessert 125 ml")).isEqualTo("Cookies & Cream");
    }

    @Test
    void fallsBackWhenNothingUseful() {
        assertThat(ProductNameParser.flavor(null)).isEqualTo("Unknown");
        assertThat(ProductNameParser.packSize("Go Zero Tub")).isNull();
    }
}
