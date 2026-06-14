package org.vippro.fraud_check_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.command.FraudCheckRequestedCommand;
import org.vippro.fraud_check_service.config.FraudRuleProperties;
import org.vippro.fraud_check_service.repository.FraudDecisionRepository;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class FraudRuleEngine {

    private final FraudRuleProperties properties;
    private final FraudDecisionRepository decisionRepository;

    public RuleResult evaluate(FraudCheckRequestedCommand command) {
        if (properties.deniedAccounts().contains(command.getAccountId())) {
            return RuleResult.deny(
                    "ACCOUNT_DENIED",
                    "Account is on the fraud denylist"
            );
        }

        BigDecimal maxAmount = properties.getMaxAmount()
                .get(command.getCurrency());
        if (maxAmount == null) {
            return RuleResult.deny(
                    "CURRENCY_NOT_CONFIGURED",
                    "No fraud threshold configured for currency "
                            + command.getCurrency()
            );
        }
        if (command.getAmount().compareTo(maxAmount) > 0) {
            return RuleResult.deny(
                    "AMOUNT_LIMIT_EXCEEDED",
                    "Payment amount exceeds the configured fraud threshold"
            );
        }

        Instant threshold = Instant.now().minusSeconds(
                properties.getVelocity().getWindowSeconds()
        );
        long recentPayments =
                decisionRepository.countByAccountIdAndCreatedAtGreaterThanEqual(
                        command.getAccountId(),
                        threshold
                );
        if (recentPayments >= properties.getVelocity().getMaxPayments()) {
            return RuleResult.deny(
                    "VELOCITY_LIMIT_EXCEEDED",
                    "Account exceeded the payment velocity limit"
            );
        }

        return RuleResult.allow();
    }

    public record RuleResult(
            boolean passed,
            String ruleCode,
            String reason
    ) {
        static RuleResult allow() {
            return new RuleResult(
                    true,
                    "ALL_RULES_PASSED",
                    "Fraud checks passed"
            );
        }

        static RuleResult deny(String ruleCode, String reason) {
            return new RuleResult(false, ruleCode, reason);
        }
    }
}
