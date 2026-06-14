package org.vippro.authorization_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationServerApplicationTests {

	private final MockMvc mockMvc;
	private final JdbcTemplate jdbcTemplate;
	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	AuthorizationServerApplicationTests(
			MockMvc mockMvc,
			JdbcTemplate jdbcTemplate,
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {
		this.mockMvc = mockMvc;
		this.jdbcTemplate = jdbcTemplate;
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
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

	@Test
	void loadsUserAndRolesFromUserServiceTables() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS users (
				    user_id UUID PRIMARY KEY,
				    username VARCHAR(50) UNIQUE NOT NULL,
				    password_hash VARCHAR(100) NOT NULL,
				    status VARCHAR(20) NOT NULL
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS user_roles (
				    user_id UUID NOT NULL,
				    role VARCHAR(20) NOT NULL
				)
				""");

		java.util.UUID userId = java.util.UUID.randomUUID();
		jdbcTemplate.update(
				"""
				INSERT INTO users (
				    user_id, username, password_hash, status
				) VALUES (?, ?, ?, ?)
				""",
				userId,
				"admin",
				passwordEncoder.encode("strong-password-123"),
				"ACTIVE"
		);
		jdbcTemplate.update(
				"INSERT INTO user_roles (user_id, role) VALUES (?, ?)",
				userId,
				"ADMIN"
		);

		var user = userDetailsService.loadUserByUsername("ADMIN");

		assertThat(user.isEnabled()).isTrue();
		assertThat(passwordEncoder.matches(
				"strong-password-123",
				user.getPassword()
		)).isTrue();
		assertThat(user.getAuthorities())
				.extracting("authority")
				.containsExactly("ROLE_ADMIN");
	}
}
