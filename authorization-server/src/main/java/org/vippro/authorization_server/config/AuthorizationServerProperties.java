package org.vippro.authorization_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "authorization")
public record AuthorizationServerProperties(
        String issuer,
        Client client,
        User user
) {
    public record Client(
            String id,
            String secret,
            String redirectUri,
            String postLogoutRedirectUri
    ) {
    }

    public record User(
            String username,
            String password
    ) {
    }
}
