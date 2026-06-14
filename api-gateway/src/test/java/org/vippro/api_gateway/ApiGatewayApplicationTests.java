package org.vippro.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"management.health.eureka.enabled=false"
})
class ApiGatewayApplicationTests {

	@Autowired
	private RouteLocator routeLocator;

	@Test
	void contextLoads() {
		assertThat(routeLocator.getRoutes()
				.map(route -> route.getId())
				.collectList()
				.block())
				.contains(
						"payment-command",
						"payment-query",
						"account-api"
				);
	}

}
