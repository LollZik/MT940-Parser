package mt940parser.parser;

import mt940parser.model.Balance;
import mt940parser.model.Statement;
import mt940parser.model.Transaction;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Mt940Parser {
    private Mt940Parser() {
    }

    private static final Set<String> VALID_UNSUPPORTED_TAGS = Set.of("21", "64", "65");
    private static final String[] VALID_ORDER = {"20", "25", "28C", "60F", "61", "62F", "86"};

    public static List<Statement> parse(Path filepath, Charset charset) throws Mt940ParseException, IOException {
        List<String> lines = Files.readAllLines(filepath, charset);
        return parse(lines);
    }

    public static List<Statement> parse(List<String> lines) throws Mt940ParseException{
        List<RawStatement> rawStatements = Tokenizer.tokenizeData(lines);
        List<Statement> statements = new ArrayList<>();

        for(RawStatement s : rawStatements){
            statements.add(parseStatement(s));
        }
        return List.copyOf(statements);
    }

    private static Statement parseStatement(RawStatement rawStatement) throws Mt940ParseException {
        int i = 0;
        int firstFieldLine = rawStatement.fields().getFirst().line();
        boolean lastWasTransaction = false;
        RawField lastTransaction = null;

        String statementReference = null;
        String accountNumber = null;
        String statementNumber = null;
        Balance openingBalance = null;
        List<Transaction> transactions = new ArrayList<>();
        Balance closingBalance = null;
        Optional<String> information = Optional.empty();

        for (RawField field : rawStatement.fields()) {
            if (i >= VALID_ORDER.length) {
                throw new Mt940ParseException("Too many tags were found, stopped at: " + field.tag(), field.line());
            }
            if (!VALID_ORDER[i].equals("61")) {
                if (!field.tag().equals(VALID_ORDER[i])) {
                    handleWrongTags(field);
                } else {
                    switch (field.tag()) {
                        case "20" -> statementReference = field.value();
                        case "25" -> accountNumber = field.value();
                        case "28C" -> statementNumber = field.value();
                        case "60F" -> openingBalance = FieldParser.parseBalance(field);
                        case "86" -> information = Optional.of(field.value());
                    }
                    i++;
                }
            } else {
                if (field.tag().equals("61")) {
                    if (lastWasTransaction) {
                        transactions.add(FieldParser.parseTransaction(lastTransaction, Optional.empty()));
                    }
                    lastWasTransaction = true;
                    lastTransaction = field;
                } else if (field.tag().equals("86")) {
                    if (!lastWasTransaction) {
                        throw new Mt940ParseException("Tag :86: encountered before :61: or :62F:", field.line());
                    }
                    transactions.add(FieldParser.parseTransaction(lastTransaction, Optional.of(field)));
                    lastWasTransaction = false;
                } else if (field.tag().equals("62F")) {
                    if (lastWasTransaction) {
                        transactions.add(FieldParser.parseTransaction(lastTransaction, Optional.empty()));
                    }
                    closingBalance = FieldParser.parseBalance(field);
                    i += 2; // Set to "86"
                } else {
                    handleWrongTags(field);
                }
            }
        }
        if (i < VALID_ORDER.length - 1) {
            String s;
            if (i == 4) {
                // 61 was optional, so if it stopped there it never got to 62F
                s = "Tag :62F: was missing";
            } else {
                s = "At least one tag was missing: " + VALID_ORDER[i];
            }
            throw new Mt940ParseException(s, firstFieldLine);
        }
        return new Statement(statementReference, accountNumber, statementNumber, openingBalance, transactions, closingBalance, information);
    }

    private static void handleWrongTags(RawField field) throws Mt940ParseException {
        if (!VALID_UNSUPPORTED_TAGS.contains(field.tag())) {
            if (Set.of(VALID_ORDER).contains(field.tag())) {
                throw new Mt940ParseException("Encountered tag out of order: " + field.tag(), field.line());
            } else {
                throw new Mt940ParseException("Unknown tag encountered: " + field.tag(), field.line());
            }
        }

    }
}
