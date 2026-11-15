package mt940parser.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTest {
    private static final BigDecimal POSITIVE = new BigDecimal("100.00");
    private static final BigDecimal NEGATIVE = new BigDecimal("-100.00");
    private static final LocalDate VALUE_DATE = LocalDate.of(2020, 10, 10);
    private static final Optional<LocalDate> ENTRY_DATE = Optional.of(LocalDate.of(2020, 10, 14));
    private static final Optional<LocalDate> EMPTY_OPTIONAL_DATE = Optional.empty();
    private static final String TYPE = "NCHG";
    private static final Optional<String> CLIENT_REF = Optional.of("ABC");
    private static final Optional<String> BANK_REF = Optional.of("DEF");
    private static final Optional<String> SUPP_DETAILS = Optional.of("Lorem ipsum");
    private static final Optional<String> INFORMATION = Optional.of("Dolor sit amet");
    private static final Optional<String> EMPTY_OPTIONAL_STRING = Optional.empty();

    @Test
    void signedAmountReturnsSign() {
        Transaction rc = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION);
        Transaction c = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, false, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION);
        Transaction rd = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.DEBIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION);
        Transaction d = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.DEBIT, false, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION);


        assertEquals(NEGATIVE, rc.signedAmount());
        assertEquals(POSITIVE, c.signedAmount());
        assertEquals(POSITIVE, rd.signedAmount());
        assertEquals(NEGATIVE, d.signedAmount());
    }

    @Test
    void rejectNegativeBigDecimal() {
        assertThrows(IllegalArgumentException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, false, NEGATIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION));
    }

    @Test
    void acceptEmptyOptionals() {
        Transaction t1 = new Transaction(VALUE_DATE, EMPTY_OPTIONAL_DATE, DebitCredit.CREDIT, false, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION);
        Transaction t2 = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, false, POSITIVE, TYPE, EMPTY_OPTIONAL_STRING, BANK_REF, SUPP_DETAILS, INFORMATION);
        Transaction t3 = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, false, POSITIVE, TYPE, CLIENT_REF, EMPTY_OPTIONAL_STRING, SUPP_DETAILS, INFORMATION);
        Transaction t4 = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, false, POSITIVE, TYPE, CLIENT_REF, BANK_REF, EMPTY_OPTIONAL_STRING, INFORMATION);
        Transaction t5 = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, false, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, EMPTY_OPTIONAL_STRING);

        assertEquals(EMPTY_OPTIONAL_DATE, t1.entryDate());
        assertEquals(EMPTY_OPTIONAL_STRING, t2.clientRef());
        assertEquals(EMPTY_OPTIONAL_STRING, t3.bankRef());
        assertEquals(EMPTY_OPTIONAL_STRING, t4.supplementaryDetails());
        assertEquals(EMPTY_OPTIONAL_STRING, t5.information());
    }


    @Test
    void rejectNullArgs() {
        assertThrows(NullPointerException.class, () -> new Transaction(null, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, null, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, null, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, null, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, null, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, null, BANK_REF, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, null, SUPP_DETAILS, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, null, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, null));
    }

}
