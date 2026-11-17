package mt940parser.validation;

import mt940parser.model.Balance;
import mt940parser.model.DebitCredit;
import mt940parser.model.Statement;
import mt940parser.model.Transaction;
import mt940parser.parser.Mt940Parser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StatementValidatorTest {

    private static final Currency PLN = Currency.getInstance("PLN");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final LocalDate OPENING_DATE = LocalDate.of(2026, 11, 1);
    private static final LocalDate CLOSING_DATE = LocalDate.of(2026, 11, 5);


    private static Balance balance(DebitCredit mark, LocalDate date, Currency currency, String amount) {
        return new Balance(mark, date, currency, new BigDecimal(amount));
    }

    private static Transaction transaction(DebitCredit mark, boolean reversal, String amount) {
        return new Transaction(OPENING_DATE, Optional.empty(), mark, reversal, new BigDecimal(amount), "NTRF",
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static Statement statement(Balance opening, Balance closing, List<Transaction> transactions) {
        return new Statement("REF1", "PL61109010140000071219812874", "00001/1", opening, transactions, closing, Optional.empty());
    }

    private static List<Statement> parseResource(String name, Charset charset) throws Exception {
        return Mt940Parser.parse(resource(name), charset);
    }

    private static Path resource(String name) throws URISyntaxException {
        URL url = StatementValidatorTest.class.getResource("/" + name);
        assertNotNull(url, "Missing test resource: " + name);
        return Path.of(url.toURI());
    }


    @Test
    void balancedStatementHasNoIssues() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "1000.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "1150.00"),
                List.of(transaction(DebitCredit.CREDIT, false, "200.00"), transaction(DebitCredit.DEBIT, false, "50.00")));

        assertTrue(StatementValidator.validateStatement(s).isEmpty());
    }

    @Test
    void statementWithoutTransactionsAndEqualBalancesHasNoIssues() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "500.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "500.00"),
                List.of());

        assertTrue(StatementValidator.validateStatement(s).isEmpty());
    }

    @Test
    void handlesDebitBalancesAndReversals() {
        Statement s = statement(
                balance(DebitCredit.DEBIT, OPENING_DATE, PLN, "250.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "850.50"),
                List.of(
                        transaction(DebitCredit.CREDIT, false, "1000"),
                        transaction(DebitCredit.DEBIT, true, "250.00"),
                        transaction(DebitCredit.CREDIT, true, "100.00"),
                        transaction(DebitCredit.DEBIT, false, "49.5")));

        assertTrue(StatementValidator.validateStatement(s).isEmpty());
    }

    @Test
    void ignoresDifferentScaleOfEqualAmounts() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "1000"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "1500.00"),
                List.of(transaction(DebitCredit.CREDIT, false, "500.0")));

        assertTrue(StatementValidator.validateStatement(s).isEmpty());
    }

    @Test
    void sameOpeningAndClosingDateIsAllowed() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "100.00"),
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "100.00"),
                List.of());

        assertTrue(StatementValidator.validateStatement(s).isEmpty());
    }

    @Test
    void reportsBalanceDiscrepancy() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "1000.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "950.00"),
                List.of(transaction(DebitCredit.DEBIT, false, "100.00")));

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertEquals(1, issues.size());
        assertEquals(IssueType.BALANCE_DISCREPANCY, issues.getFirst().issue());
        assertSame(s, issues.getFirst().statement());
        assertTrue(issues.getFirst().message().contains("50.00"));
    }

    @Test
    void reportsBalanceDiscrepancyWithoutTransactions() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "500.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "600.00"),
                List.of());

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertEquals(1, issues.size());
        assertEquals(IssueType.BALANCE_DISCREPANCY, issues.getFirst().issue());
    }

    @Test
    void reportsClosingDateBeforeOpeningDate() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "100.00"),
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "100.00"),
                List.of());

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertEquals(1, issues.size());
        assertEquals(IssueType.DATES_DISCREPANCY, issues.getFirst().issue());
    }

    @Test
    void reportsCurrencyDiscrepancy() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "100.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, EUR, "999.00"),
                List.of());

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertEquals(1, issues.size());
        assertEquals(IssueType.CURRENCY_DISCREPANCY, issues.getFirst().issue());
        assertTrue(issues.getFirst().message().contains("PLN"));
        assertTrue(issues.getFirst().message().contains("EUR"));
    }

    @Test
    void reportsMultipleIssuesAtOnce() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "100.00"),
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "200.00"),
                List.of());

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertEquals(2, issues.size());
        assertTrue(issues.stream().anyMatch(i -> i.issue() == IssueType.DATES_DISCREPANCY));
        assertTrue(issues.stream().anyMatch(i -> i.issue() == IssueType.BALANCE_DISCREPANCY));
    }

    @Test
    void returnedListIsUnmodifiable() {
        Statement s = statement(
                balance(DebitCredit.CREDIT, OPENING_DATE, PLN, "100.00"),
                balance(DebitCredit.CREDIT, CLOSING_DATE, PLN, "100.00"),
                List.of());

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertThrows(UnsupportedOperationException.class,
                () -> issues.add(new ValidationIssue(IssueType.BALANCE_DISCREPANCY, s, "test")));
    }


    @Test
    void validFilesHaveNoIssues() throws Exception {
        for (String name : List.of("valid_single.sta", "multiple_statements.sta", "edge_cases.sta", "utf8_bom_crlf.sta")) {
            for (Statement s : parseResource(name, StandardCharsets.UTF_8)) {
                assertTrue(StatementValidator.validateStatement(s).isEmpty(), "Unexpected issue in " + name);
            }
        }
        Statement polish = parseResource("polish_chars_cp1250_crlf.sta", Charset.forName("windows-1250")).getFirst();
        assertTrue(StatementValidator.validateStatement(polish).isEmpty());
    }

    @Test
    void balanceMismatchFileReportsBalanceDiscrepancy() throws Exception {
        Statement s = parseResource("balance_mismatch.sta", StandardCharsets.UTF_8).getFirst();

        List<ValidationIssue> issues = StatementValidator.validateStatement(s);

        assertEquals(1, issues.size());
        assertEquals(IssueType.BALANCE_DISCREPANCY, issues.getFirst().issue());
        assertTrue(issues.getFirst().message().contains("50.00"));
    }
}