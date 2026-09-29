package com.gozero.ingestion.scraper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsTxtPolicyTest {

    private static final String ROBOTS = """
            User-agent: Googlebot
            Disallow: /

            User-agent: *
            Disallow: /checkout/
            Disallow: /*?sort=
            Allow: /checkout/help
            """;

    @Test
    void appliesOnlyWildcardGroup() {
        RobotsTxtPolicy policy = RobotsTxtPolicy.parse(ROBOTS);
        assertThat(policy.isAllowed("/pb/go-zero/")).isTrue();
        assertThat(policy.isAllowed("/checkout/cart")).isFalse();
    }

    @Test
    void longestMatchWinsAndWildcardsWork() {
        RobotsTxtPolicy policy = RobotsTxtPolicy.parse(ROBOTS);
        assertThat(policy.isAllowed("/checkout/help")).isTrue();
        assertThat(policy.isAllowed("/pb/go-zero/?sort=price")).isFalse();
    }

    @Test
    void emptyPolicyAllowsEverything() {
        assertThat(RobotsTxtPolicy.allowAll().isAllowed("/anything")).isTrue();
    }
}
