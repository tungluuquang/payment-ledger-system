package org.vippro.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"management.health.eureka.enabled=false"
})
class ApiGatewayApplicationTests {

	@Autowired
	private RouteLocator routeLocator;

	@MockBean
	private ReactiveJwtDecoder jwtDecoder;

	@Test
	void contextLoads() {
		assertThat(routeLocator.getRoutes()
				.map(route -> route.getId())
				.collectList()
				.block())
				.contains(
						"payment-command",
						"payment-query",
						"account-api",
						"user-api"
				);
	}

	@Test
	void gatewayAndSpringWebAreBinaryCompatible() {
		assertThatCode(() -> HttpHeaders.class.getMethod("headerSet"))
				.doesNotThrowAnyException();
	}

}
