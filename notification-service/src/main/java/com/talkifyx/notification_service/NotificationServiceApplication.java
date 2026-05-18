package com.talkifyx.notification_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class NotificationServiceApplication {

    public static void main(String[] args) {

        try {
            Dotenv dotenv = Dotenv.configure()
                    // .directory("./notification-service")
                    .directory("./")
                    .ignoreIfMissing()
                    .ignoreIfMalformed()
                    .load();

            if (dotenv.entries().isEmpty()) {
                System.out.println(".env not found or empty");
            } else {
                dotenv.entries().forEach(e -> {
                    if (e.getValue() != null && !e.getValue().isBlank()) {
                        System.setProperty(e.getKey(), e.getValue());
                        System.out.println(e.getKey() + " = [" + e.getValue() + "]");
                    } else {
                        System.out.println("Missing value for key: " + e.getKey());
                    }
                });

                System.out.println(".env loaded successfully");
            }

            System.out.println("MAIL_USERNAME = " + System.getProperty("MAIL_USERNAME"));
            System.out.println("MAIL_PASSWORD = " + System.getProperty("MAIL_PASSWORD"));
            System.out.println("EUREKA_URL = " + System.getProperty("EUREKA_URL"));

        } catch (Exception e) {
            System.out.println("Failed to load .env file");
            e.printStackTrace();
        }

        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}