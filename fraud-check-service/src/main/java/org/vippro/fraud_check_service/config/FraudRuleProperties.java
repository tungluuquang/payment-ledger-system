package org.vippro.fraud_check_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@ConfigurationProperties(prefix = "fraud.rules")
public class FraudRuleProperties {

    private String version = "1";
    private Map<CurrencyType, BigDecimal> maxAmount =
            new EnumMap<>(CurrencyType.class);
    private Velocity velocity = new Velocity();
    private String deniedAccountIds = "";

    public Set<UUID> deniedAccounts() {
        if (deniedAccountIds == null || deniedAccountIds.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(deniedAccountIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Getter
    @Setter
    public static class Velocity {
        private long windowSeconds = 3600;
        private long maxPayments = 10;
    }
}
