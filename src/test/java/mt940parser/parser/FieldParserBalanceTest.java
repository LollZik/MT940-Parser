package mt940parser.parser;

import mt940parser.model.Balance;
import mt940parser.model.DebitCredit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldParserBalanceTest {

    private static final int LINE = 4;

    // Valid values
    private static final String CREDIT_BALANCE = "C261101PLN10000,00";
    private static final String DEBIT_BALANCE = "D261230PLN250,00";
    private static final String AMOUNT_WITHOUT_DECIMALS = "C261101PLN1500,";
    private static final String AMOUNT_WITH_ONE_DECIMAL = "C261101PLN49,5";
    private static final String ZERO_AMOUNT = "C261101PLN0,00";

    private static final String AMOUNT_WITHOUT_COMMA = "C261101PLN1500";
    private static final String LOWERCASE_CURRENCY = "C261101pln1500,00";
    private static final String UNKNOWN_MARK = "X261101PLN1500,00";
    private static final String TRAILING_GARBAGE = "C261101PLN1500,00XYZ";
    private static final String NON_EXISTENT_DATE = "C261332PLN1500,00";
    private static final String FEBRUARY_30 = "C260230PLN1500,00";
    private static final String UNKNOWN_CURRENCY = "C261101XYZ1500,00";
    private static final String EMPTY_VALUE = "";

    private static final LocalDate EXPECTED_CREDIT_DATE = LocalDate.of(2026, 11, 1);
    private static final LocalDate EXPECTED_DEBIT_DATE = LocalDate.of(2026, 12, 30);
    private static final Currency PLN = Currency.getInstance("PLN");


    private static RawField balanceField(String value) {
        return new RawField("60F", value, LINE);
    }


    @Test
    void parsesCreditBalance() throws Mt940ParseException {
        Balance b = FieldParser.parseBalance(balanceField(CREDIT_BALANCE));
        assertEquals(EXPECTED_CREDIT_DATE, b.date());
        assertEquals(DebitCredit.CREDIT, b.mark());
        assertEquals(PLN, b.currency());
        assertEquals(new BigDecimal("10000.00"), b.amount());
        assertEquals(new BigDecimal("10000.00"), b.signedAmount());

    }

    @Test
    void parsesDebitBalance() throws Mt940ParseException {
        Balance b = FieldParser.parseBalance(balanceField(DEBIT_BALANCE));
        assertEquals(EXPECTED_DEBIT_DATE, b.date());
        assertEquals(DebitCredit.DEBIT, b.mark());
        assertEquals(PLN, b.currency());
        assertEquals(new BigDecimal("250.00"), b.amount());
        assertEquals(new BigDecimal("-250.00"), b.signedAmount());
    }

    @Test
    void parsesAmountWithoutDecimals() throws Mt940ParseException {
        Balance b = FieldParser.parseBalance(balanceField(AMOUNT_WITHOUT_DECIMALS));
        assertEquals(EXPECTED_CREDIT_DATE, b.date());
        assertEquals(DebitCredit.CREDIT, b.mark());
        assertEquals(PLN, b.currency());
        assertEquals(new BigDecimal("1500"), b.amount());
    }

    @Test
    void parsesAmountWithOneDecimal() throws Mt940ParseException {
        Balance b = FieldParser.parseBalance(balanceField(AMOUNT_WITH_ONE_DECIMAL));
        assertEquals(EXPECTED_CREDIT_DATE, b.date());
        assertEquals(DebitCredit.CREDIT, b.mark());
        assertEquals(PLN, b.currency());
        assertEquals(new BigDecimal("49.5"), b.amount());
    }

    @Test
    void parsesZeroAmount() throws Mt940ParseException {
        Balance b = FieldParser.parseBalance(balanceField(ZERO_AMOUNT));
        assertEquals(EXPECTED_CREDIT_DATE, b.date());
        assertEquals(DebitCredit.CREDIT, b.mark());
        assertEquals(PLN, b.currency());
        assertEquals(new BigDecimal("0.00"), b.amount());
    }


    @Test
    void rejectsAmountWithoutComma() {
        assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(AMOUNT_WITHOUT_COMMA)));
    }

    @Test
    void rejectsLowercaseCurrency() {
        assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(LOWERCASE_CURRENCY)));
    }

    @Test
    void rejectsUnknownMark() {
        assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(UNKNOWN_MARK)));
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(TRAILING_GARBAGE)));
    }

    @Test
    void rejectsEmptyValue() {
        assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(EMPTY_VALUE)));
    }


    @Test
    void rejectsNonExistentDate() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(NON_EXISTENT_DATE)));
        assertEquals(LINE, e.getLine());
        assertInstanceOf(DateTimeException.class, e.getCause());
    }

    @Test
    void rejectsIncorrectDate() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(FEBRUARY_30)));
        assertEquals(LINE, e.getLine());
        assertInstanceOf(DateTimeException.class, e.getCause());
    }

    @Test
    void rejectsUnknownCurrency() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class, () -> FieldParser.parseBalance(balanceField(UNKNOWN_CURRENCY)));
        assertEquals(LINE, e.getLine());
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
    }
}