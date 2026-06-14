package org.vippro.authorization_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.vippro.authorization_server.config.AuthorizationServerProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthorizationServerProperties.class)
public class AuthorizationServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationServerApplication.class, args);
	}

}
