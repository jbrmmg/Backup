package com.jbr.middletier.backup.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI backupOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backup Manager API")
                        .description("REST API for backup management, directory synchronisation, and media import.")
                        .version("v1")
                        .contact(new Contact()
                                .name("jbrmmg")
                                .email("jbrmmg2011@gmail.com")));
    }
}
