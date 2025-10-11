package lexer;

public enum TokenType {
    // Keywords
    GLOB, PROC, FUNC, MAIN, VAR, LOCAL, RETURN,
    HALT, PRINT, WHILE, DO, UNTIL, IF, ELSE,
    
    // Operators
    NEG, NOT, EQ, GT, OR, AND, PLUS, MINUS, MULT, DIV,
    
    // Delimiters
    LPAREN, RPAREN, LBRACE, RBRACE,
    SEMICOLON, COMMA, ASSIGN,
    
    // Literals and identifiers
    IDENTIFIER, NUMBER, STRING,
    
    // Special
    EOF, UNKNOWN
}