package org.vippro.authorization_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
class AuthorizationServerApplicationTests {

	private final MockMvc mockMvc;

	@Autowired
	AuthorizationServerApplicationTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void exposesOpenIdProviderConfiguration() throws Exception {
		mockMvc.perform(get("/.well-known/openid-configuration"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issuer").value("http://localhost:9000"))
				.andExpect(jsonPath("$.jwks_uri").exists());
	}

	@Test
	void issuesAccessTokenForClientCredentials() throws Exception {
		mockMvc.perform(post("/oauth2/token")
						.with(httpBasic("payment-ledger-client", "change-me"))
						.param("grant_type", "client_credentials")
						.param("scope", "payment.read"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.access_token").isNotEmpty())
				.andExpect(jsonPath("$.token_type").value("Bearer"))
				.andExpect(jsonPath("$.scope").value("payment.read"));
	}

}
