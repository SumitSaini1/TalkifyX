package com.talkifyx.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.configure()
			.directory("./auth-service")
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

		System.out.println("JWT_SECRET = " + dotenv.get("JWT_SECRET"));
		System.out.println("PROFILE = " + System.getProperty("SPRING_PROFILES_ACTIVE"));
		System.out.println("MAIL = " + System.getProperty("MAIL_USERNAME"));

		SpringApplication.run(AuthServiceApplication.class, args);
	}
}