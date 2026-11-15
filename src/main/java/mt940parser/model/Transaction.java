package mt940parser.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record Transaction(LocalDate valueDate, Optional<LocalDate> entryDate,
                          DebitCredit mark, boolean reversal, BigDecimal amount,
                          String transactionType, Optional<String> clientRef, Optional<String> bankRef,
                          Optional<String> supplementaryDetails, Optional<String> information) {
    public Transaction {
        Objects.requireNonNull(valueDate, "Value date must not be null");
        Objects.requireNonNull(entryDate, "Entry date must not be null");
        Objects.requireNonNull(mark, "Mark must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(transactionType, "Transaction type must not be null");
        Objects.requireNonNull(clientRef, "Client reference must not be null");
        Objects.requireNonNull(bankRef, "Bank reference must not be null");
        Objects.requireNonNull(supplementaryDetails, "Supplementary details reference must not be null");
        Objects.requireNonNull(information, "Information must not be null");

        if (amount.signum() < 0) throw new IllegalArgumentException("Amount's value can't be negative, was: " + amount);
    }

    public BigDecimal signedAmount() {
        BigDecimal result = mark.applyTo(amount);
        return reversal ? result.negate() : result;
    }
}