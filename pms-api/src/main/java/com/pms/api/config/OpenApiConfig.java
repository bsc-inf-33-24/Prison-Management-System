package com.pms.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI prisonManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prison Management System API")
                        .version("v1")
                        .description("Canonical documentation for the Prison Management System business API. All business endpoints are published under the /api/v1 prefix.")
                        .contact(new Contact().name("Prison Management System Team"))
                        .license(new License().name("Internal Use Only")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
