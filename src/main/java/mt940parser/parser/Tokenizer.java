package mt940parser.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Tokenizer {
    private static final Pattern TAG_PATTERN = Pattern.compile("^:(\\d\\d[A-Z]?):");

    private Tokenizer() {
    }

    public static List<RawStatement> tokenizeData(List<String> data) throws Mt940ParseException {
        boolean openedField = false;
        String currTag = "";
        StringBuilder currValue = new StringBuilder();
        int currLine = 0;
        int lastLine = -1;

        Matcher m;
        ArrayList<RawField> fieldsList = new ArrayList<>();
        ArrayList<RawStatement> statements = new ArrayList<>();

        for (String s : data) {
            currLine++;

            if (currLine == 1 && s.startsWith("\uFEFF")) { // BOM
                s = s.substring(1);
            }

            m = TAG_PATTERN.matcher(s);
            if (m.lookingAt()) {
                if (openedField) {
                    fieldsList.add(new RawField(currTag, currValue.toString(), lastLine));
                    currValue = new StringBuilder();
                }
                currTag = m.group(1);
                currValue.append(s.substring(m.end()));

                openedField = true;
                lastLine = currLine;
            } else if (s.strip().equals("-")) {
                if (openedField) {
                    fieldsList.add(new RawField(currTag, currValue.toString(), lastLine));
                    statements.add(new RawStatement(fieldsList));
                    openedField = false;
                }
                fieldsList.clear();
                currValue = new StringBuilder();
            } else if (s.isBlank()) {
                continue;
            } else {
                if (openedField) {
                    currValue.append("\n");
                    currValue.append(s);
                }
                else{
                    throw new Mt940ParseException("Text found before any field tag.", currLine);
                }
            }
        }
        if (openedField) { // Last statement didn't end with '-'
            fieldsList.add(new RawField(currTag, currValue.toString(), lastLine));
            statements.add(new RawStatement(fieldsList));
        }
        return statements;
    }
}
