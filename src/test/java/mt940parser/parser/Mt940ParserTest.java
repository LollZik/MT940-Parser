package mt940parser.parser;

import mt940parser.model.DebitCredit;
import mt940parser.model.Statement;
import mt940parser.model.Transaction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

class Mt940ParserTest {

    private static final Currency PLN = Currency.getInstance("PLN");
    private static final Currency EUR = Currency.getInstance("EUR");

    private static final String REF = ":20:REF1";
    private static final String ACCOUNT = ":25:PL61109010140000071219812874";
    private static final String NUMBER = ":28C:00001/1";
    private static final String OPENING = ":60F:C261101PLN100,00";
    private static final String TRANSACTION = ":61:2611021102D10,00NTRFNONREF";
    private static final String INFORMATION = ":86:Some information";
    private static final String CLOSING = ":62F:C261102PLN90,00";
    private static final String END = "-";


    private static Path resource(String name) throws URISyntaxException {
        URL url = Mt940ParserTest.class.getResource("/" + name);
        assertNotNull(url, "Missing test resource: " + name);
        return Path.of(url.toURI());
    }

    private static Statement parseSingle(String name, Charset charset) throws Exception {
        List<Statement> statements = Mt940Parser.parse(resource(name), charset);
        assertEquals(1, statements.size());
        return statements.getFirst();
    }

    @Test
    void parsesHeaderAndBalancesOfValidSingleFile() throws Exception {
        Statement s = parseSingle("valid_single.sta", StandardCharsets.UTF_8);

        assertEquals("REF20261101", s.statementReference());
        assertEquals("PL61109010140000071219812874", s.accountNumber());
        assertEquals("00042/1", s.statementNumber());

        assertEquals(DebitCredit.CREDIT, s.openingBalance().mark());
        assertEquals(LocalDate.of(2026, 11, 1), s.openingBalance().date());
        assertEquals(PLN, s.openingBalance().currency());
        assertEquals(new BigDecimal("10000.00"), s.openingBalance().amount());

        assertEquals(LocalDate.of(2026, 11, 6), s.closingBalance().date());
        assertEquals(new BigDecimal("10988.20"), s.closingBalance().amount());

        assertEquals(Optional.empty(), s.information());
    }

    @Test
    void parsesTransactionsOfValidSingleFile() throws Exception {
        Statement s = parseSingle("valid_single.sta", StandardCharsets.UTF_8);
        assertEquals(3, s.transactions().size());

        Transaction first = s.transactions().get(0);
        assertEquals(DebitCredit.DEBIT, first.mark());
        assertEquals(new BigDecimal("1500.00"), first.amount());
        assertEquals(Optional.of("INV2026001"), first.clientRef());
        assertEquals(Optional.of("BANK000001"), first.bankRef());
        assertEquals(Optional.of("020~00PRZELEW WYCHODZACY~20Faktura FV/10/2026~32JAN KOWALSKI\n~38PL12100000000000000000000001"),
                first.information());

        Transaction second = s.transactions().get(1);
        assertEquals(DebitCredit.CREDIT, second.mark());
        assertEquals(new BigDecimal("2500.50"), second.amount());
        assertEquals(Optional.empty(), second.clientRef());
        assertEquals(Optional.of("ZWROT NADPLATY"), second.supplementaryDetails());
        assertEquals(Optional.of("051~00PRZELEW PRZYCHODZACY~20Zwrot nadplaty za pazdziernik"), second.information());

        Transaction third = s.transactions().get(2);
        assertEquals("NCHG", third.transactionType());
        assertEquals(new BigDecimal("12.30"), third.amount());
        assertEquals(Optional.of("BANK000002"), third.bankRef());
    }

    @Test
    void parsesMultipleStatements() throws Exception {
        List<Statement> statements = Mt940Parser.parse(resource("multiple_statements.sta"), StandardCharsets.UTF_8);
        assertEquals(2, statements.size());

        Statement first = statements.get(0);
        assertEquals("REF20261101A", first.statementReference());
        assertEquals(PLN, first.closingBalance().currency());
        assertEquals(new BigDecimal("6000.00"), first.closingBalance().amount());
        assertEquals(1, first.transactions().size());

        Statement second = statements.get(1);
        assertEquals("REF20261101B", second.statementReference());
        assertEquals("PL12100000000000000000000002", second.accountNumber());
        assertEquals(EUR, second.openingBalance().currency());
        assertEquals(new BigDecimal("149.75"), second.closingBalance().amount());
        assertEquals(1, second.transactions().size());
    }

    @Test
    void parsesEdgeCasesFile() throws Exception {
        Statement s = parseSingle("edge_cases.sta", StandardCharsets.UTF_8);

        assertEquals(DebitCredit.DEBIT, s.openingBalance().mark());
        assertEquals(new BigDecimal("850.50"), s.closingBalance().amount());
        assertEquals(Optional.of("Informacja do calego wyciagu"), s.information());
        assertEquals(4, s.transactions().size());

        Transaction rollover = s.transactions().get(0);
        assertEquals(LocalDate.of(2026, 12, 31), rollover.valueDate());
        assertEquals(Optional.of(LocalDate.of(2027, 1, 2)), rollover.entryDate());
        assertEquals(new BigDecimal("1000"), rollover.amount());

        Transaction reversalDebit = s.transactions().get(1);
        assertTrue(reversalDebit.reversal());
        assertEquals(new BigDecimal("250.00"), reversalDebit.signedAmount());
        assertEquals(Optional.of("073~00STORNO~20Zwrot oplaty"), reversalDebit.information());

        Transaction reversalCredit = s.transactions().get(2);
        assertTrue(reversalCredit.reversal());
        assertEquals(new BigDecimal("-100.00"), reversalCredit.signedAmount());
        assertEquals(Optional.empty(), reversalCredit.information());

        Transaction oneDecimal = s.transactions().get(3);
        assertEquals(new BigDecimal("49.5"), oneDecimal.amount());
        assertEquals(Optional.of("073~00OPLATA~20Oplata roczna"), oneDecimal.information());
    }

    @Test
    void parsesFileWithBalanceMismatchWithoutError() throws Exception {
        Statement s = parseSingle("balance_mismatch.sta", StandardCharsets.UTF_8);
        assertEquals(new BigDecimal("950.00"), s.closingBalance().amount());
        assertEquals(1, s.transactions().size());
    }

    @Test
    void parsesWindows1250File() throws Exception {
        Statement s = parseSingle("polish_chars_cp1250_crlf.sta", Charset.forName("windows-1250"));
        Transaction t = s.transactions().getFirst();
        assertTrue(t.information().orElseThrow().contains("WYCHODZĄCY"));
        assertTrue(t.information().orElseThrow().contains("SKLEP ŁOŚ SP. Z O.O."));
        assertEquals(new BigDecimal("254.01"), s.closingBalance().amount());
    }

    @Test
    void parsesUtf8FileWithBom() throws Exception {
        Statement s = parseSingle("utf8_bom_crlf.sta", StandardCharsets.UTF_8);
        assertEquals("REF20261111", s.statementReference());
        assertTrue(s.transactions().getFirst().information().orElseThrow().contains("SKLEP ŁOŚ SP. Z O.O."));
    }

    @Test
    void parsesStatementWithoutTransactions() throws Mt940ParseException {
        List<Statement> statements = Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, OPENING, CLOSING, END));
        assertEquals(1, statements.size());
        assertTrue(statements.getFirst().transactions().isEmpty());
    }

    @Test
    void emptyInputGivesNoStatements() throws Mt940ParseException {
        assertTrue(Mt940Parser.parse(List.of()).isEmpty());
    }

    @Test
    void returnedListIsUnmodifiable() throws Mt940ParseException {
        List<Statement> statements = Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, OPENING, CLOSING, END));
        assertThrows(UnsupportedOperationException.class, () -> statements.add(statements.getFirst()));
    }


    @Test
    void rejectsFileWithMissingClosingBalance() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(resource("missing_closing_balance.sta"), StandardCharsets.UTF_8));
        assertEquals(1, e.getLine());
        assertTrue(e.getMessage().contains("62F"));
    }

    @Test
    void rejectsMissingStatementNumber() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ACCOUNT, END)));
        assertEquals(1, e.getLine());
        assertTrue(e.getMessage().contains("28C"));
    }

    @Test
    void reportsFirstLineOfStatementForMissingTagInSecondStatement() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, OPENING, CLOSING, END, REF, ACCOUNT, END)));
        assertEquals(7, e.getLine());
    }

    @Test
    void rejectsTransactionBeforeOpeningBalance() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, TRANSACTION, OPENING, CLOSING, END)));
        assertEquals(4, e.getLine());
    }

    @Test
    void rejectsDuplicatedReference() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, REF, ACCOUNT, NUMBER, OPENING, CLOSING, END)));
        assertEquals(2, e.getLine());
    }

    @Test
    void rejectsUnknownTag() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ":99:UNKNOWN", ACCOUNT, NUMBER, OPENING, CLOSING, END)));
        assertEquals(2, e.getLine());
    }

    @Test
    void rejectsInformationWithoutTransaction() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, OPENING, INFORMATION, CLOSING, END)));
        assertEquals(5, e.getLine());
    }

    @Test
    void rejectsFieldAfterStatementInformation() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, OPENING, CLOSING, INFORMATION, INFORMATION, END)));
        assertEquals(7, e.getLine());
    }

    @Test
    void rejectsTransactionAfterClosingBalance() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Mt940Parser.parse(List.of(REF, ACCOUNT, NUMBER, OPENING, CLOSING, TRANSACTION, END)));
        assertEquals(6, e.getLine());
    }

    @Test
    void ignoresUnsupportedTags() throws Mt940ParseException {
        List<Statement> statements = Mt940Parser.parse(List.of(
                REF, ":21:NONREF", ACCOUNT, NUMBER, OPENING, CLOSING, ":64:C261102PLN90,00", ":65:C261103PLN90,00", END));
        assertEquals(1, statements.size());
    }

    @Test
    void throwsIoExceptionForMissingFile() {
        assertThrows(IOException.class,
                () -> Mt940Parser.parse(Path.of("does_not_exist.sta"), StandardCharsets.UTF_8));
    }
}