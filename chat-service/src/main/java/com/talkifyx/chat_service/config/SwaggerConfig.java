package com.talkifyx.chat_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI chatServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat Service API")
                        .description("TalkifyX WebSocket Chat Service")
                        .version("1.0.0"));
    }
}