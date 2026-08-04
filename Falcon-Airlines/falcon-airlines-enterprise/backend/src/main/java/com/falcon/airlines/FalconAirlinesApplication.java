package com.falcon.airlines;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Falcon Airlines Spring Boot backend.
 * Phase 1: foundation only. Business auto-configuration is intentionally disabled
 * until the respective modules are implemented.
 */
@SpringBootApplication
public class FalconAirlinesApplication {

    public static void main(String[] args) {
        SpringApplication.run(FalconAirlinesApplication.class, args);
    }
}
