package mt940parser.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizerTest {

    private static List<String> readResource(String name, Charset charset) throws IOException, URISyntaxException {
        URL url = TokenizerTest.class.getResource("/" + name);
        assertNotNull(url, "Missing test resource: " + name);
        return Files.readAllLines(Path.of(url.toURI()), charset);
    }

    private static RawField field(RawStatement statement, int index) {
        return statement.fields().get(index);
    }

    @Test
    void splitsSingleLineFieldsIntoTagAndValue() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":20:REF1",
                ":25:PL61109010140000071219812874",
                ":28C:00001/1",
                ":60F:C261101PLN1,00",
                "-"));

        assertEquals(1, result.size());
        RawStatement statement = result.getFirst();
        assertEquals(4, statement.fields().size());

        assertEquals("20", field(statement, 0).tag());
        assertEquals("REF1", field(statement, 0).value());
        assertEquals("28C", field(statement, 2).tag());
        assertEquals("00001/1", field(statement, 2).value());
        assertEquals("60F", field(statement, 3).tag());
        assertEquals("C261101PLN1,00", field(statement, 3).value());
    }

    @Test
    void fieldWithEmptyValueIsKept() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(":20:", ":25:ACC", "-"));

        RawStatement statement = result.getFirst();
        assertEquals(2, statement.fields().size());
        assertEquals("20", field(statement, 0).tag());
        assertEquals("", field(statement, 0).value());
    }

    @Test
    void appendsContinuationLineWithNewline() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":86:first line",
                "second line",
                "third line",
                "-"));

        RawField info = field(result.getFirst(), 0);
        assertEquals("86", info.tag());
        assertEquals("first line\nsecond line\nthird line", info.value());
    }

    @Test
    void colonTagInsideContinuationIsNotTreatedAsNewField() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":86:Payment",
                "Invoice nr :20: ABC",
                "-"));

        RawStatement statement = result.getFirst();
        assertEquals(1, statement.fields().size());
        assertEquals("Payment\nInvoice nr :20: ABC", field(statement, 0).value());
    }

    @Test
    void dashSplitsFileIntoStatements() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":20:FIRST",
                ":25:ACC1",
                "-",
                ":20:SECOND",
                "-"));

        assertEquals(2, result.size());
        assertEquals(2, result.getFirst().fields().size());
        assertEquals(1, result.get(1).fields().size());
        assertEquals("SECOND", field(result.get(1), 0).value());
    }

    @Test
    void valueDoesNotLeakIntoNextStatement() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":62F:C261103PLN6000,00",
                "-",
                ":20:REF2",
                "-"));

        assertEquals("REF2", field(result.get(1), 0).value());
    }

    @Test
    void dashWithTrailingSpacesEndsStatement() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(":20:A", "-   "));

        assertEquals(1, result.size());
        assertEquals("A", field(result.getFirst(), 0).value());
    }

    @Test
    void missingFinalDashProducesStatement() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(":20:A", ":25:B"));

        assertEquals(1, result.size());
        assertEquals(2, result.getFirst().fields().size());
        assertEquals("B", field(result.getFirst(), 1).value());
    }

    @Test
    void emptyInputGivesNoStatements() throws Mt940ParseException {
        assertTrue(Tokenizer.tokenizeData(List.of()).isEmpty());
    }

    @Test
    void skipsWhitespaceOnlyLines() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":20:A",
                "",
                "   \t",
                ":25:B",
                "-"));

        RawStatement statement = result.getFirst();
        assertEquals(2, statement.fields().size());
        assertEquals("A", field(statement, 0).value());
        assertEquals("B", field(statement, 1).value());
    }

    @Test
    void fieldRemembersLineWhereItStarts() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":20:A",
                ":86:first",
                "continuation",
                ":62F:Z",
                "-"));

        RawStatement statement = result.getFirst();
        assertEquals(1, field(statement, 0).line());
        assertEquals(2, field(statement, 1).line());
        assertEquals(4, field(statement, 2).line());
    }

    @Test
    void blankLineAtFileStartCountsTowardsLineNumbers() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                "",
                ":20:A",
                "-"));

        assertEquals(2, field(result.getFirst(), 0).line());
    }

    @Test
    void blankLineBetweenStatementsCountsTowardsLineNumbers() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of(
                ":20:A",
                "-",
                "",
                ":20:B",          // line 4
                "-"));

        assertEquals(4, field(result.get(1), 0).line());
    }


    @Test
    void textBeforeFirstTagThrows() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Tokenizer.tokenizeData(List.of("garbage", ":20:A", "-")));

        assertEquals(1, e.getLine());
    }

    @Test
    void textAfterDashBeforeNextTagThrows() {
        Mt940ParseException e = assertThrows(Mt940ParseException.class,
                () -> Tokenizer.tokenizeData(List.of(":20:A", "-", "garbage", ":20:B")));

        assertEquals(3, e.getLine());
    }


    @Test
    void removesBomFromFirstLine() throws Mt940ParseException {
        List<RawStatement> result = Tokenizer.tokenizeData(List.of("\uFEFF:20:A", "-"));

        assertEquals(1, result.size());
        assertEquals("20", field(result.getFirst(), 0).tag());
        assertEquals("A", field(result.getFirst(), 0).value());
    }

    @Test
    void tokenizesValidSingleFile() throws Exception {
        List<RawStatement> result = Tokenizer.tokenizeData(readResource("valid_single.sta", StandardCharsets.UTF_8));

        assertEquals(1, result.size());
        RawStatement statement = result.getFirst();
        assertEquals(11, statement.fields().size());

        RawField multilineInfo = field(statement, 5);
        assertEquals("86", multilineInfo.tag());
        assertEquals(6, multilineInfo.line());
        assertTrue(multilineInfo.value().endsWith("\n~38PL12100000000000000000000001"));

        RawField lineWithSupplementaryDetails = field(statement, 6);
        assertEquals("61", lineWithSupplementaryDetails.tag());
        assertEquals(8, lineWithSupplementaryDetails.line());
        assertEquals("2611051105C2500,50NTRFNONREF\nZWROT NADPLATY", lineWithSupplementaryDetails.value());

        RawField closingBalance = field(statement, 10);
        assertEquals("62F", closingBalance.tag());
        assertEquals("C261106PLN10988,20", closingBalance.value());
    }

    @Test
    void tokenizesMultipleStatementsFile() throws Exception {
        List<RawStatement> result = Tokenizer.tokenizeData(readResource("multiple_statements.sta", StandardCharsets.UTF_8));

        assertEquals(2, result.size());
        assertEquals(7, result.getFirst().fields().size());
        assertEquals(7, result.get(1).fields().size());

        RawField secondReference = field(result.get(1), 0);
        assertEquals("REF20261101B", secondReference.value());
        assertEquals(9, secondReference.line());
    }

    @Test
    void tokenizesEdgeCasesFile() throws Exception {
        List<RawStatement> result = Tokenizer.tokenizeData(readResource("edge_cases.sta", StandardCharsets.UTF_8));

        assertEquals(1, result.size());
        RawStatement statement = result.getFirst();
        assertEquals(16, statement.fields().size());

        RawField firstTransaction = field(statement, 5);
        assertEquals("61", firstTransaction.tag());
        assertEquals(7, firstTransaction.line());

        RawField statementInfo = field(statement, 15);
        assertEquals("86", statementInfo.tag());
        assertEquals("Informacja do calego wyciagu", statementInfo.value());
    }

    @Test
    void tokenizesUtf8FileWithBomAndCrlf() throws Exception {
        List<RawStatement> result = Tokenizer.tokenizeData(readResource("utf8_bom_crlf.sta", StandardCharsets.UTF_8));

        RawStatement statement = result.getFirst();
        assertEquals("20", field(statement, 0).tag());
        assertEquals("REF20261111", field(statement, 0).value());
        assertTrue(field(statement, 5).value().contains("SKLEP ŁOŚ SP. Z O.O."));
    }

    @Test
    void tokenizesWindows1250FileWithCrlf() throws Exception {
        List<RawStatement> result = Tokenizer.tokenizeData(readResource("polish_chars_cp1250_crlf.sta", Charset.forName("windows-1250")));

        RawStatement statement = result.getFirst();
        assertEquals(7, statement.fields().size());
        assertTrue(field(statement, 5).value().contains("WYCHODZĄCY"));
        assertTrue(field(statement, 5).value().contains("SKLEP ŁOŚ SP. Z O.O."));
    }
}