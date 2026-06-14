package org.vippro.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.vippro.user_service.config.UserBootstrapProperties;

@SpringBootApplication
@EnableConfigurationProperties(UserBootstrapProperties.class)
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

}
