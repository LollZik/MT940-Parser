package mt940parser.validation;

import mt940parser.model.Statement;
import mt940parser.model.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StatementValidator {
    private StatementValidator() {
    }

    public static List<ValidationIssue> validateStatement(Statement statement) {
        List<ValidationIssue> issues = new ArrayList<>();
        Optional<ValidationIssue> issue;

        issue = validateDates(statement);
        issue.ifPresent(issues::add);

        issue = validateBalances(statement);
        issue.ifPresent(issues::add);

        return List.copyOf(issues);
    }

    private static Optional<ValidationIssue> validateDates(Statement statement) {
        if (statement.closingBalance().date().isBefore(statement.openingBalance().date())) {
            return Optional.of(new ValidationIssue(IssueType.DATES_DISCREPANCY, statement, "Closing date is earlier than opening date"));
        }
        return Optional.empty();
    }

    private static Optional<ValidationIssue> validateBalances(Statement statement) {
        if (!statement.openingBalance().currency().equals(statement.closingBalance().currency())) {
            return Optional.of(new ValidationIssue(IssueType.CURRENCY_DISCREPANCY, statement, "Expected same currencies, got " + statement.openingBalance().currency() + " and " + statement.closingBalance().currency()));
        }
        BigDecimal expectedDifference = statement.closingBalance().signedAmount().subtract(statement.openingBalance().signedAmount());
        BigDecimal transactionSum = BigDecimal.ZERO;
        for (Transaction t : statement.transactions()) {
            transactionSum = transactionSum.add(t.signedAmount());
        }
        if (expectedDifference.compareTo(transactionSum) == 0) {
            return Optional.empty();
        } else {
            String s = "Expected " + expectedDifference + ", got " + transactionSum + ", difference: " + expectedDifference.subtract(transactionSum);
            return Optional.of(new ValidationIssue(IssueType.BALANCE_DISCREPANCY, statement, s));
        }
    }
}
