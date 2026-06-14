package org.vippro.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/users")
                        .permitAll()
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/users/*/status"
                        ).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/users")
                        .hasRole("ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/users/**")
                        .hasAuthority("SCOPE_user.read")
                        .pathMatchers(
                                HttpMethod.PATCH,
                                "/api/users/**"
                        ).hasAuthority("SCOPE_user.write")
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/users/*/password"
                        ).hasAuthority("SCOPE_user.write")
                        .pathMatchers(HttpMethod.POST, "/api/payments")
                        .hasAuthority("SCOPE_payment.write")
                        .pathMatchers(HttpMethod.GET, "/api/payments/**")
                        .hasAuthority("SCOPE_payment.read")
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/accounts",
                                "/api/accounts/**"
                        )
                        .hasAuthority("SCOPE_account.read")
                        .pathMatchers(HttpMethod.POST, "/api/accounts")
                        .hasAuthority("SCOPE_account.write")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter()
                        ))
                )
                .build();
    }

    private ReactiveJwtAuthenticationConverterAdapter
    jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes =
                new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities =
                    new ArrayList<>(scopes.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority(
                                "ROLE_" + role
                        ))
                        .forEach(authorities::add);
            }
            return authorities;
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
