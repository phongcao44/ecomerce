package com.ra.base_spring_boot.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI api(@Value("${open.api.title}") String title,
                       @Value("${open.api.version}") String version,
                       @Value("${open.api.description}") String description,
                       @Value("${open.api.serverUrl}") String serverUrl
    ) {
        return new OpenAPI().info(new Info()
                .title(title)
                .version(version)
                .description(description))
                .servers(List.of(new Server().url(serverUrl).description("Test")))
                 .components(new Components().
                        addSecuritySchemes("bearerAuth", new SecurityScheme().
                                type(SecurityScheme.Type.HTTP).
                                scheme("bearer").
                                bearerFormat("JWT"))).
                                security(List.of(new SecurityRequirement().addList("bearerAuth")));
    }
    @Bean
    public GroupedOpenApi customOpenAPI() {
        return GroupedOpenApi.builder()
                .group("api-service-1")
                .packagesToScan("com.ra.base_spring_boot.controller")
                .build();
    }
}
