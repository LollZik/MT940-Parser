package mt940parser.parser;

import mt940parser.model.Balance;
import mt940parser.model.DebitCredit;
import mt940parser.model.Transaction;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;

final class FieldParser {
    private static final Pattern BALANCE_PATTERN = Pattern.compile("^([CD])(\\d{6})([A-Z]{3})(\\d+,\\d*)");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile("^(?<valueDate>\\d{6})(?<entryDate>\\d{4})?(?<mark>R?[CD])[A-Z]?(?<amount>\\d+,\\d*)(?<type>[A-Z][A-Z0-9]{3})(?<clientRef>.{1,16}?)(?://(?<bankRef>.{1,16}?))?(?:\n(?<suppDetails>.{1,34}))?");
    private static final int MAX_MONTH_DIFF = 10;

    private FieldParser() {
    }

    static Balance parseBalance(RawField field) throws Mt940ParseException {
        LocalDate date;
        Currency currency;
        DebitCredit mark;
        BigDecimal amount;

        String value = field.value();
        Matcher m = BALANCE_PATTERN.matcher(value);
        if (!m.matches()) {
            throw new Mt940ParseException("Given balance info format is invalid: " + value, field.line());
        }

        mark = (m.group(1).equals("C") ? DebitCredit.CREDIT : DebitCredit.DEBIT);

        date = parseDate(m.group(2), field.line());

        try {
            currency = Currency.getInstance(m.group(3));
        } catch (IllegalArgumentException e) {
            throw new Mt940ParseException("Currency format is invalid: " + m.group(3), field.line(), e);
        }

        amount = parseMoney(m.group(4));


        return new Balance(mark, date, currency, amount);
    }

    static Transaction parseTransaction(RawField field, Optional<RawField> information) throws Mt940ParseException {
        String value = field.value();
        Matcher m = TRANSACTION_PATTERN.matcher(value);
        if (!m.matches()) {
            throw new Mt940ParseException("Given transaction info format is invalid: " + value, field.line());
        }

        LocalDate valueDate = parseDate(m.group("valueDate"), field.line());

        Optional<LocalDate> entryDate;
        Optional<String> entry = Optional.ofNullable(m.group(("entryDate")));
        if (entry.isEmpty()) {
            entryDate = Optional.empty();
        } else {
            int month = Integer.parseInt(entry.get().substring(0, 2));
            int day = Integer.parseInt(entry.get().substring(2, 4));
            try{
                if (abs(month - valueDate.getMonthValue()) > MAX_MONTH_DIFF) {
                    if (month > valueDate.getMonthValue()) {
                        entryDate = Optional.of(LocalDate.of(valueDate.getYear() - 1, month, day));
                    } else {
                        entryDate = Optional.of(LocalDate.of(valueDate.getYear() + 1, month, day));
                    }
                } else {
                    entryDate = Optional.of(LocalDate.of(valueDate.getYear(), month, day));
                }
            }
            catch(DateTimeException e){
                throw new Mt940ParseException("Date format is invalid: " + entry.get(), field.line(), e);
            }
        }

        String strMark = m.group("mark");
        DebitCredit mark = strMark.endsWith("C") ? DebitCredit.CREDIT : DebitCredit.DEBIT;
        boolean reversal = strMark.startsWith("R");

        BigDecimal amount = parseMoney(m.group("amount"));

        String transactionType = m.group("type");

        Optional<String> clientRef;
        String clientReference = (m.group("clientRef"));
        if(clientReference.equals("NONREF")){
            clientRef = Optional.empty();
        } else {
            clientRef = Optional.of(clientReference);
        }
        Optional<String> bankRef = Optional.ofNullable(m.group("bankRef"));

        Optional<String> supplementaryDetails = Optional.ofNullable(m.group("suppDetails"));

        Optional<String> info = information.isEmpty() ? Optional.empty() : Optional.of(information.get().value());


        return new Transaction(valueDate, entryDate, mark, reversal, amount, transactionType, clientRef, bankRef, supplementaryDetails, info);
    }

    private static LocalDate parseDate(String rawDate, int fieldLine) throws Mt940ParseException {

        int year = 2000 + Integer.parseInt(rawDate.substring(0, 2));
        int month = Integer.parseInt(rawDate.substring(2, 4));
        int day = Integer.parseInt(rawDate.substring(4, 6));

        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            throw new Mt940ParseException("Date format is invalid: " + rawDate, fieldLine, e);
        }
    }


    private static BigDecimal parseMoney(String amount) {
        // No try-catch is needed, regex guarantees correct format
        return new BigDecimal(amount.replace(',', '.'));
    }
}
