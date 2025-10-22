package parser;

/**
 * Custom exception class for handling syntax errors during parsing.
 */
public class ParserException extends Exception {
    private final int line;
    private final int column;

    /**
     * Constructor for a generic parser error without specific location data.
     * @param message The error message.
     */
    public ParserException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    /**
     * Constructor for a parser error with specific location data.
     * @param message The error message.
     * @param line The line number where the error occurred.
     * @param column The column number where the error occurred.
     */
    public ParserException(String message, int line, int column) {
        super(String.format("Parser Error: %s. Found error at Line %d, Column %d", message, line, column));
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
