package org.vippro.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "user.bootstrap.admin")
public record UserBootstrapProperties(
        boolean enabled,
        String username,
        String email,
        String password,
        String fullName
) {
}
