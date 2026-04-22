package com.talkifyx.message_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableFeignClients
public class MessageServiceApplication {

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
				.directory("./message-service")
                .ignoreIfMissing()
                .load();

        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
      
        System.setProperty("EUREKA_URL", dotenv.get("EUREKA_URL"));

        SpringApplication.run(MessageServiceApplication.class, args);
    }
}