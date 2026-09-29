package com.gozero.reconciliation;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "Go Zero - Reconciliation Service (SIMULATED DATA)",
        version = "1.0.0",
        description = "Cross-platform price/stock reconciliation. Blinkit, Zepto and Instamart listings are SIMULATED "
                + "(no public access); only the BigBasket reference price comes from ingestion-service."))
public class ReconciliationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationApplication.class, args);
    }
}
