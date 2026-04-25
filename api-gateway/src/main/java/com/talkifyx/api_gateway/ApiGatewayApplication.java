package com.talkifyx.api_gateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
                .directory("./api-gateway")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        System.out.println("PROFILE = " + System.getProperty("SPRING_PROFILES_ACTIVE"));
        System.out.println("EUREKA_URL = " + System.getProperty("EUREKA_URL"));

        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}