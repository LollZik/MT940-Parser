package mt940parser.cli;

import mt940parser.model.Balance;
import mt940parser.model.Statement;
import mt940parser.model.Transaction;
import mt940parser.parser.Mt940ParseException;
import mt940parser.parser.Mt940Parser;
import mt940parser.validation.StatementValidator;
import mt940parser.validation.ValidationIssue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class Main {
    private static final String TRANSACTION_ROW = "  %-10s  %-10s  %14s  %-4s  %.50s%n";
    private static final int EXIT_OK = 0;
    private static final int EXIT_VALIDATION_ISSUES = 1;
    private static final int EXIT_INVALID_FILE = 2;
    private static final int EXIT_USAGE_OR_IO_ERROR = 3;

    static void main(String[] args) {
        if (args.length == 0 || args.length > 2) {
            printUsage();
            System.exit(EXIT_USAGE_OR_IO_ERROR);
        }

        String charset = args.length == 2 ? args[1] : "UTF-8";

        try {
            List<Statement> statements = parseInputFile(args[0], charset);
            boolean issuesOccurred = printAndValidateStatements(statements);

            System.exit(issuesOccurred ? EXIT_VALIDATION_ISSUES : EXIT_OK);
        } catch (Mt940ParseException e) {
            System.err.println("Invalid file: " + e.getMessage());
            System.exit(EXIT_INVALID_FILE);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid argument: " + e.getMessage());
            System.exit(EXIT_USAGE_OR_IO_ERROR);
        } catch (IOException e) {
            System.err.println("Cannot read file: " + e.getMessage());
            System.exit(EXIT_USAGE_OR_IO_ERROR);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar mt940-parser.jar <file> [charset]");
        System.err.println("If no charset is given, UTF-8 is used.");
    }

    private static List<Statement> parseInputFile(String fileName, String charset) throws Mt940ParseException, IOException {
        return Mt940Parser.parse(Path.of(fileName), Charset.forName(charset));
    }

    private static boolean printAndValidateStatements(List<Statement> statements) {
        boolean issuesOccurred = false;

        for (Statement statement : statements) {
            printHeader(statement);
            printBody(statement);

            for (ValidationIssue issue : StatementValidator.validateStatement(statement)) {
                System.err.println("  WARNING " + issue.issue() + ": " + issue.message());
                issuesOccurred = true;
            }
            System.out.println();
        }
        return issuesOccurred;
    }

    private static void printHeader(Statement statement) {
        System.out.printf("Statement %s (%s)%n", statement.statementReference(), statement.statementNumber());
        System.out.printf("Account:  %s%n", statement.accountNumber());
        System.out.printf("Opening:  %s%n", formatBalance(statement.openingBalance()));
        System.out.printf("Closing:  %s%n%n", formatBalance(statement.closingBalance()));
    }

    private static void printBody(Statement statement) {
        System.out.printf(TRANSACTION_ROW, "Value date", "Entry date", "Amount", "Type", "Description");

        for (Transaction t : statement.transactions()) {
            System.out.printf(TRANSACTION_ROW,
                    t.valueDate(),
                    t.entryDate().map(Object::toString).orElse("-"),
                    formatAmount(t.signedAmount(), true),
                    t.transactionType(),
                    firstLine(t.information().orElse("")));
        }
        System.out.printf("  %d transaction(s)%n", statement.transactions().size());
    }

    private static String formatBalance(Balance balance) {
        return String.format("%s  %14s %s", balance.date(), formatAmount(balance.signedAmount(), false), balance.currency());
    }

    private static String formatAmount(java.math.BigDecimal amount, boolean withSign) {
        return String.format(Locale.ROOT, withSign ? "%+,.2f" : "%,.2f", amount);
    }

    private static String firstLine(String text) {
        return text.lines().findFirst().orElse("");
    }
}