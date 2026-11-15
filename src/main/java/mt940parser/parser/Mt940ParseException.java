package mt940parser.parser;

public class Mt940ParseException extends Exception {
    private final int exceptionLine;

    public Mt940ParseException(String description, int line) {
        super(description + " Exception triggered by Line: " + line + " of the file.");

        this.exceptionLine = line;
    }

    public int getLine() {
        return exceptionLine;
    }
}
