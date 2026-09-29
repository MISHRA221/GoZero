package com.gozero.ingestion.scraper;

public class ScrapeBlockedException extends RuntimeException {

    public ScrapeBlockedException(String message) {
        super(message);
    }
}
