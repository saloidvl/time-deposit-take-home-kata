package org.ikigaidigital.adapter.controller.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class OpenApiConfig {

    // The relative server URL keeps the generated contract the same on any host (D12).
    @Bean
    OpenAPI timeDepositsApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("XA Bank Time Deposits API")
                        .version("1.0")
                        .description("Reads time deposits and runs the monthly balance update."))
                .servers(List.of(new Server().url("/")));
    }

    // Every field of every response is always present, so the contract marks them all as required.
    @Bean
    OpenApiCustomizer allFieldsRequired() {
        return openApi -> openApi.getComponents().getSchemas().values().forEach(this::requireAllProperties);
    }

    private void requireAllProperties(Schema<?> schema) {
        schema.setRequired(List.copyOf(schema.getProperties().keySet()));
    }
}
