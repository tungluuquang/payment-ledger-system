package org.vippro.event;

import lombok.Data;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class AccountDebitRequested {
    private UUID paymentId;
    private UUID accountId;
    private BigDecimal amount;
    private CurrencyType currency;
    private LocalDateTime localDateTime;

}
