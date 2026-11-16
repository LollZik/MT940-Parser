package mt940parser.parser;

public class Mt940ParseException extends Exception {
    private final int exceptionLine;


    public Mt940ParseException(String description, int line, Throwable cause) {
        super(description + " Exception triggered by Line: " + line + " of the file.", cause);
        exceptionLine = line;
    }

    public Mt940ParseException(String description, int line) {
        this(description, line, null);
    }


    public int getLine() {
        return exceptionLine;
    }
}
