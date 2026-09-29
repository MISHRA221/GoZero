package com.gozero.reconciliation.domain;

public enum Platform {
    BIGBASKET("BigBasket"),
    BLINKIT("Blinkit"),
    ZEPTO("Zepto"),
    INSTAMART("Swiggy Instamart");

    private final String displayName;

    Platform(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
