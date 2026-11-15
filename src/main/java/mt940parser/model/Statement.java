package mt940parser.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Statement(String statementReference, String accountNumber, String statementNumber,
                        Balance openingBalance, List<Transaction> transactions,
                        Balance closingBalance, Optional<String> information) {
    public Statement {
        Objects.requireNonNull(statementReference, "Statement reference must not be null");
        Objects.requireNonNull(accountNumber, "Account number must not be null");
        Objects.requireNonNull(statementNumber, "Statement number must not be null");
        Objects.requireNonNull(openingBalance, "Opening balance must not be null");
        Objects.requireNonNull(transactions, "Transaction list must not be null");
        Objects.requireNonNull(closingBalance, "Closing balance must not be null");
        Objects.requireNonNull(information, "Information must not be null");

        transactions = List.copyOf(transactions);
    }
}
