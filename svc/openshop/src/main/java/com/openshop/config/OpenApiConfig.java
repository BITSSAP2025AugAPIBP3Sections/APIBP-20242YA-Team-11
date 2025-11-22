package com.openshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenShop API")
                        .version("1.0.0")
                        .description("Comprehensive REST API documentation for OpenShop e-commerce platform. " +
                                "This API provides endpoints for user management, product catalog, shopping cart, " +
                                "order processing, inventory management, payments, shipping, and notifications.")
                        .contact(new Contact()
                                .name("OpenShop Team")
                                .email("support@openshop.com")
                                .url("https://openshop.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://openshop-backend.cfapps.eu12.hana.ondemand.com")
                                .description("Production Server")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT authentication token. Obtain by calling /api/users/login endpoint.")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
