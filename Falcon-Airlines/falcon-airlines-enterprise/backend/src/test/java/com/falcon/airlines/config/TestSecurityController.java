package com.falcon.airlines.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dummy controller used by {@link SecurityConfigTest} to exercise the
 * {@link org.springframework.security.web.SecurityFilterChain} endpoints.
 */
@RestController
public class TestSecurityController {

    @GetMapping("/auth/ping")
    public String authPing() {
        return "pong";
    }

    @GetMapping("/swagger-ui/index.html")
    public String swaggerUi() {
        return "swagger";
    }

    @GetMapping("/v3/api-docs")
    public String apiDocs() {
        return "openapi";
    }

    @GetMapping("/actuator/health")
    public String actuatorHealth() {
        return "UP";
    }

    @GetMapping("/api/flights")
    public String flights() {
        return "[]";
    }
}
