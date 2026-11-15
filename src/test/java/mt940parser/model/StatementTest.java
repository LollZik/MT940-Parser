package mt940parser.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StatementTest {
    private static final BigDecimal POSITIVE = new BigDecimal("100.00");
    private static final LocalDate VALUE_DATE = LocalDate.of(2020, 10, 10);
    private static final Optional<LocalDate> ENTRY_DATE = Optional.of(LocalDate.of(2020, 10, 14));
    private static final String TYPE = "NCHG";
    private static final Optional<String> CLIENT_REF = Optional.of("ABC");
    private static final Optional<String> BANK_REF = Optional.of("DEF");
    private static final Optional<String> SUPP_DETAILS = Optional.of("Lorem ipsum");
    private static final Optional<String> INFORMATION = Optional.of("Dolor sit amet");
    private static final Optional<String> EMPTY_OPTIONAL_STRING = Optional.empty();
    private static final Currency PLN = Currency.getInstance("PLN");

    private static final Transaction TRANSACTION = new Transaction(VALUE_DATE, ENTRY_DATE, DebitCredit.CREDIT, true, POSITIVE, TYPE, CLIENT_REF, BANK_REF, SUPP_DETAILS, INFORMATION);
    private static final Balance BALANCE = new Balance(DebitCredit.CREDIT, VALUE_DATE, PLN, POSITIVE);

    private static final String STR_NUMBER = "123456";
    private static final String STR_REF = "ABCDEFGH";
    private static final List<Transaction> TRANSACTIONS_LIST = new ArrayList<>(List.of(TRANSACTION, TRANSACTION, TRANSACTION));
    private static final List<Transaction> TRANSACTIONS_LIST_WITH_NULL = new ArrayList<>(Arrays.asList(TRANSACTION, null, TRANSACTION));
    private static final List<Transaction> TRANSACTIONS_EMPTY_LIST = new ArrayList<>();


    @Test
    void acceptEmptyOptionals() {
        Statement s1 = new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, BALANCE, EMPTY_OPTIONAL_STRING);
        assertEquals(EMPTY_OPTIONAL_STRING, s1.information());
    }

    @Test
    void rejectTransactionListsWithNullsInside() {
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST_WITH_NULL, BALANCE, EMPTY_OPTIONAL_STRING));
    }

    @Test
    void acceptNonNullTransactionLists() {
        Statement s1 = new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, BALANCE, EMPTY_OPTIONAL_STRING);
        Statement s2 = new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_EMPTY_LIST, BALANCE, EMPTY_OPTIONAL_STRING);

        assertEquals(TRANSACTIONS_LIST, s1.transactions());
        assertEquals(TRANSACTIONS_EMPTY_LIST, s2.transactions());
    }

    @Test
    void listStaysIndependent() {
        List<Transaction> tList = new ArrayList<>(List.of(TRANSACTION, TRANSACTION, TRANSACTION));
        int initSize = tList.size();
        Statement s1 = new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, tList, BALANCE, EMPTY_OPTIONAL_STRING);
        tList.add(TRANSACTION);
        assertEquals(initSize, s1.transactions().size());
    }

    @Test
    void cannotModifyList() {
        Statement s1 = new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, BALANCE, EMPTY_OPTIONAL_STRING);
        assertThrows(UnsupportedOperationException.class, () -> s1.transactions().add(TRANSACTION));
    }


    @Test
    void rejectNullArgs() {
        assertThrows(NullPointerException.class, () -> new Statement(null, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, BALANCE, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, null, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, BALANCE, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, STR_NUMBER, null, BALANCE, TRANSACTIONS_LIST, BALANCE, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, STR_NUMBER, STR_NUMBER, null, TRANSACTIONS_LIST, BALANCE, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, null, BALANCE, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, null, INFORMATION));
        assertThrows(NullPointerException.class, () -> new Statement(STR_REF, STR_NUMBER, STR_NUMBER, BALANCE, TRANSACTIONS_LIST, BALANCE, null));

    }
}
