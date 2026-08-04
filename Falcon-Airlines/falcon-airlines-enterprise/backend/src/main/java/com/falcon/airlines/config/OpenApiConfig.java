package com.falcon.airlines.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / SpringDoc configuration. Adds project metadata and a placeholder
 * for JWT bearer authentication that will be activated in Phase 2.
 */
@Configuration
public class OpenApiConfig {

    public static final String JWT_SECURITY = "bearer-jwt";

    @Bean
    public OpenAPI falconAirlinesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Falcon Airlines Enterprise API")
                        .description("Production-grade airline reservation platform")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Falcon Airlines Engineering")
                                .email("engineering@falconairlines.com"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(JWT_SECURITY, new SecurityScheme()
                                .name(JWT_SECURITY)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
