package mt940parser.parser;

import mt940parser.model.DebitCredit;
import mt940parser.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FieldParserTransactionTest {

    private static final int LINE = 5;

    // Valid values
    private static final String FULL_LINE = "2611021102D1500,00NTRFINV2026001//BANK000001";
    private static final String NONREF_LINE = "2611051105C2500,50NTRFNONREF";
    private static final String WITH_SUPPLEMENTARY_DETAILS = "2611051105C2500,50NTRFNONREF\nZWROT NADPLATY";
    private static final String REVERSAL_DEBIT = "261231RD250,00NCHGNONREF//X99";
    private static final String REVERSAL_CREDIT = "261231RC100,00NTRFNONREF";
    private static final String ENTRY_DATE_NEXT_YEAR = "2612310102C1000,NTRFNONREF";
    private static final String ENTRY_DATE_PREVIOUS_YEAR = "2701021231C1000,00NTRFNONREF";
    private static final String ENTRY_DATE_BEFORE_VALUE_DATE = "2603150310C100,00NTRFNONREF";
    private static final String WITH_FUNDS_CODE_AND_SLASHES = "2611021102CN1500,00NTRFINV/2026/001";

    // Invalid values
    private static final String UNKNOWN_MARK = "2611021102X1500,00NTRFNONREF";
    private static final String MISSING_CLIENT_REF = "2611021102D1500,00NTRF";
    private static final String CLIENT_REF_TOO_LONG = "2611021102D1500,00NTRFTHISREFISTOOLONG17";
    private static final String AMOUNT_WITHOUT_COMMA = "2611021102D1500NTRFNONREF";
    private static final String INVALID_VALUE_DATE = "2613011301C100,00NTRFNONREF";
    private static final String INVALID_ENTRY_DATE = "2611021332C100,00NTRFNONREF";
    private static final String SUPPLEMENTARY_DETAILS_TOO_LONG = "2611021102C100,00NTRFNONREF\n" + "X".repeat(35);
    private static final String TWO_SUPPLEMENTARY_LINES = "2611021102C100,00NTRFNONREF\nFIRST\nSECOND";

    // :86: field
    private static final Optional<RawField> INFORMATION =
            Optional.of(new RawField("86", "020~00PRZELEW~20Faktura FV/10/2026", LINE + 1));
    private static final Optional<RawField> NO_INFORMATION = Optional.empty();


    private static RawField transactionField(String value) {
        return new RawField("61", value, LINE);
    }


    @Test
    void parsesAllComponentsOfFullLine() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(FULL_LINE), NO_INFORMATION);

        assertEquals(LocalDate.of(2026, 11, 2), t.valueDate());
        assertEquals(Optional.of(LocalDate.of(2026, 11, 2)), t.entryDate());
        assertEquals(DebitCredit.DEBIT, t.mark());
        assertFalse(t.reversal());
        assertEquals(new BigDecimal("1500.00"), t.amount());
        assertEquals("NTRF", t.transactionType());
        assertEquals(Optional.of("INV2026001"), t.clientRef());
        assertEquals(Optional.of("BANK000001"), t.bankRef());
        assertEquals(Optional.empty(), t.supplementaryDetails());
        assertEquals(Optional.empty(), t.information());
    }

    @Test
    void mapsNonrefToEmptyClientRef() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(NONREF_LINE), NO_INFORMATION);
        assertEquals(Optional.empty(), t.clientRef());
    }

    @Test
    void missingBankRefIsEmpty() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(NONREF_LINE), NO_INFORMATION);
        assertEquals(Optional.empty(), t.bankRef());
    }

    @Test
    void parsesSupplementaryDetails() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(WITH_SUPPLEMENTARY_DETAILS), NO_INFORMATION);
        assertEquals(Optional.of("ZWROT NADPLATY"), t.supplementaryDetails());
        assertEquals(Optional.empty(), t.clientRef());
    }

    @Test
    void missingSupplementaryDetailsIsEmpty() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(NONREF_LINE), NO_INFORMATION);
        assertEquals(Optional.empty(), t.supplementaryDetails());
    }

    @Test
    void missingEntryDateIsEmpty() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(REVERSAL_DEBIT), NO_INFORMATION);
        assertEquals(Optional.empty(), t.entryDate());
    }

    @Test
    void parsesReversalOfDebit() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(REVERSAL_DEBIT), NO_INFORMATION);
        assertEquals(DebitCredit.DEBIT, t.mark());
        assertTrue(t.reversal());
        assertEquals(new BigDecimal("250.00"), t.signedAmount());
        assertEquals(Optional.of("X99"), t.bankRef());
    }

    @Test
    void parsesReversalOfCredit() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(REVERSAL_CREDIT), NO_INFORMATION);
        assertEquals(DebitCredit.CREDIT, t.mark());
        assertTrue(t.reversal());
        assertEquals(new BigDecimal("-100.00"), t.signedAmount());
    }

    @Test
    void entryDateRollsOverToNextYear() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(ENTRY_DATE_NEXT_YEAR), NO_INFORMATION);
        assertEquals(LocalDate.of(2026, 12, 31), t.valueDate());
        assertEquals(Optional.of(LocalDate.of(2027, 1, 2)), t.entryDate());
    }

    @Test
    void entryDateRollsBackToPreviousYear() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(ENTRY_DATE_PREVIOUS_YEAR), NO_INFORMATION);
        assertEquals(LocalDate.of(2027, 1, 2), t.valueDate());
        assertEquals(Optional.of(LocalDate.of(2026, 12, 31)), t.entryDate());
    }

    @Test
    void entryDateBeforeValueDateStaysInSameYear() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(ENTRY_DATE_BEFORE_VALUE_DATE), NO_INFORMATION);
        assertEquals(LocalDate.of(2026, 3, 15), t.valueDate());
        assertEquals(Optional.of(LocalDate.of(2026, 3, 10)), t.entryDate());
    }

    @Test
    void skipsFundsCodeAndKeepsSlashesInClientRef() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(WITH_FUNDS_CODE_AND_SLASHES), NO_INFORMATION);
        assertEquals(DebitCredit.CREDIT, t.mark());
        assertEquals(new BigDecimal("1500.00"), t.amount());
        assertEquals(Optional.of("INV/2026/001"), t.clientRef());
        assertEquals(Optional.empty(), t.bankRef());
    }

    @Test
    void attachesInformation() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(NONREF_LINE), INFORMATION);
        assertEquals(Optional.of("020~00PRZELEW~20Faktura FV/10/2026"), t.information());
    }

    @Test
    void missingInformationIsEmpty() throws Mt940ParseException {
        Transaction t = FieldParser.parseTransaction(transactionField(NONREF_LINE), NO_INFORMATION);
        assertEquals(Optional.empty(), t.information());
    }


    @Test
    void rejectsUnknownMark() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(UNKNOWN_MARK), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
    }

    @Test
    void rejectsMissingClientRef() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(MISSING_CLIENT_REF), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
    }

    @Test
    void rejectsTooLongClientRef() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(CLIENT_REF_TOO_LONG), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
    }

    @Test
    void rejectsAmountWithoutComma() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(AMOUNT_WITHOUT_COMMA), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
    }

    @Test
    void rejectsInvalidValueDate() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(INVALID_VALUE_DATE), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
        assertInstanceOf(DateTimeException.class, e.getCause());
    }

    @Test
    void rejectsInvalidEntryDate() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(INVALID_ENTRY_DATE), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
        assertInstanceOf(DateTimeException.class, e.getCause());
    }

    @Test
    void rejectsTooLongSupplementaryDetails() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(SUPPLEMENTARY_DETAILS_TOO_LONG), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
    }

    @Test
    void rejectsMoreThanOneSupplementaryLine() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseTransaction(transactionField(TWO_SUPPLEMENTARY_LINES), NO_INFORMATION));
        assertEquals(LINE, e.getLine());
    }
}