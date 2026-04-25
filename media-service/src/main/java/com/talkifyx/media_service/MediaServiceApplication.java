package com.talkifyx.media_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MediaServiceApplication {

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
                .directory("./media-service")   
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });


        System.out.println("AWS_ACCESS_KEY = " + dotenv.get("AWS_ACCESS_KEY"));
        System.out.println("AWS_SECRET_KEY = " + dotenv.get("AWS_SECRET_KEY"));

        SpringApplication.run(MediaServiceApplication.class, args);
    }
}