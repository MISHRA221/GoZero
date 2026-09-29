package com.gozero.ingestion;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "Go Zero - Ingestion Service",
        version = "1.0.0",
        description = "Scrapes Go Zero product listings from bigbasket.com/pb/go-zero/ (robots.txt-aware, rate limited). "
                + "Falls back to a bundled seed dataset when scraping is blocked; every response carries isLiveData."))
public class IngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionApplication.class, args);
    }
}
