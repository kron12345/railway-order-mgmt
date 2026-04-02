package com.ordermgmt.railway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** OpenAPI/Swagger configuration for the Path Manager REST API. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pathManagerOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Railway Path Manager API")
                                .description(
                                        "REST API for the simplified TTT Path Manager. "
                                                + "Simulates communication between Order Management "
                                                + "and Infrastructure Manager (IM) for capacity "
                                                + "requests, offers, and bookings.")
                                .version("1.0.0"));
    }
}
