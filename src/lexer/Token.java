package lexer;

public class Token {
    private TokenType type;
    private String lexeme;
    private int line;
    private int column;
    
    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }
    
    // Getters
    public TokenType getType() { return type; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    
    @Override
    public String toString() {
        return String.format("Token(%s, '%s', %d:%d)", 
            type, lexeme, line, column);
    }
}