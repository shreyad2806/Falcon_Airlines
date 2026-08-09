package com.falcon.airlines.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / SpringDoc configuration. Adds project metadata to the generated
 * Swagger UI and JWT Bearer security scheme.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI falconAirlinesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Falcon Airlines Enterprise API")
                        .description("Production-grade airline reservation platform — Phase 4")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Falcon Airlines Engineering")
                                .email("engineering@falconairlines.com"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer token authentication")));
    }
}
