package com.guilhermedev.librarymedia.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI personalMediaLibraryApi() {
        return new OpenAPI().info(new Info()
                .title("Personal Media Library API")
                .version("v1")
                .description("REST API for managing personal book, movie, and series libraries."));
    }
}
