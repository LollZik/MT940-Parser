package mt940parser.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.time.LocalDate;
import java.util.Objects;

public record Balance(DebitCredit mark, LocalDate date, Currency currency, BigDecimal amount) {
    public Balance {
        Objects.requireNonNull(mark, "Mark must not be null");
        Objects.requireNonNull(date, "Date must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");

        if (amount.signum() < 0) throw new IllegalArgumentException("Amount's value can't be negative, was: " + amount);
    }

    public BigDecimal signedAmount() {
        return mark.applyTo(amount);
    }
}