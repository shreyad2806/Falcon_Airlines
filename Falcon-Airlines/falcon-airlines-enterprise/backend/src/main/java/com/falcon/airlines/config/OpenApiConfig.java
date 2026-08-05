package com.falcon.airlines.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / SpringDoc configuration. Adds project metadata to the generated
 * Swagger UI. Security will be added in Phase 2 once authentication is implemented.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI falconAirlinesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Falcon Airlines Enterprise API")
                        .description("Production-grade airline reservation platform — Phase 1 foundation")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Falcon Airlines Engineering")
                                .email("engineering@falconairlines.com"))
                        .license(new License().name("MIT")));
    }
}
