package com.eventura.showservice.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO : update when openAI added

// @Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventuraOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Eventura Show Service API")
                        .description("API documentation for managing shows")
                        .version("1.0"));
    }
}
