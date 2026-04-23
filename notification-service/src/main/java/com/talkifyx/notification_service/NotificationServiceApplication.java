package com.talkifyx.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableFeignClients
public class NotificationServiceApplication {

    public static void main(String[] args) {

        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            if (dotenv.entries().isEmpty()) {
                System.out.println(".env file not found or empty — skipping environment loading");
            } else {
                dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
                System.out.println(".env loaded successfully");
            }

        } catch (Exception e) {
            System.out.println("Failed to load .env file");
            e.printStackTrace();
        }

        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}