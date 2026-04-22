package com.talkifyx.media_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI mediaServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Media Service API")
                .version("1.0")
                .description("TalkifyX Media/File Service"));
    }
}