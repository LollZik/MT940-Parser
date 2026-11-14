package mt940parser.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {
    private static final BigDecimal POSITIVE = new BigDecimal("100.00");
    private static final BigDecimal NEGATIVE = new BigDecimal("-100.00");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final LocalDate DATE = LocalDate.of(2020, 10, 10);
    private static final Currency PLN = Currency.getInstance("PLN");


    @Test
    void validBalanceMembers() {
        Balance b1 = new Balance(DebitCredit.CREDIT, DATE, PLN, POSITIVE);
        Balance b2 = new Balance(DebitCredit.DEBIT, DATE, PLN, POSITIVE);
        Balance b3 = new Balance(DebitCredit.DEBIT, DATE, PLN, ZERO);

        assertEquals(DebitCredit.CREDIT, b1.mark());
        assertEquals(DebitCredit.DEBIT, b2.mark());

        assertEquals(POSITIVE, b1.amount());
        assertEquals(POSITIVE, b2.amount());
        assertEquals(ZERO, b3.amount());

        assertEquals(DATE, b1.date());
        assertEquals(PLN, b1.currency());
    }

    @Test
    void signedAmountReturnsSign() {
        Balance b1 = new Balance(DebitCredit.CREDIT, DATE, PLN, POSITIVE);
        Balance b2 = new Balance(DebitCredit.DEBIT, DATE, PLN, POSITIVE);


        assertEquals(POSITIVE, b1.signedAmount());
        assertEquals(NEGATIVE, b2.signedAmount());
    }

    @Test
    void rejectNegativeBigDecimal() {
        assertThrows(IllegalArgumentException.class, () -> new Balance(DebitCredit.CREDIT, DATE, PLN, NEGATIVE));
    }

    @Test
    void rejectNullArgs() {
        assertThrows(NullPointerException.class, () -> new Balance(null, DATE, PLN, POSITIVE));
        assertThrows(NullPointerException.class, () -> new Balance(DebitCredit.CREDIT, null, PLN, POSITIVE));
        assertThrows(NullPointerException.class, () -> new Balance(DebitCredit.CREDIT, DATE, null, POSITIVE));
        assertThrows(NullPointerException.class, () -> new Balance(DebitCredit.CREDIT, DATE, PLN, null));
    }
}