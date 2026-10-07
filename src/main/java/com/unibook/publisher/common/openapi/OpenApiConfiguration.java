package com.unibook.publisher.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI unibookOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniBook Publisher API")
                        .description("RESTful API for managing the publishing workflow: manuscripts, chapters and revisions, "
                                + "feedback threads, cover design, author contracts and royalties, notifications and users")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UniBook Publisher Team")));
    }
}
