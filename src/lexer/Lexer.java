package lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Lexer {
    private String source;
    private int position;
    private int line;
    private int column;
    private List<Token> tokens;
    
    public Lexer(String source) {
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
    }
    
    public List<Token> tokenize() throws LexerException {
        while (position < source.length()) {
            skipWhitespace();
            if (position >= source.length()) break;
            
            Token token = nextToken();
            tokens.add(token);
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }
    
    private Token nextToken() throws LexerException {
        char current = peek();
        
        // identifiers/keywords
        if (isLetter(current)) {
            return scanIdentifierOrKeyword();
        }
        
        // numbers
        if (isDigit(current)) {
            return scanNumber();
        }
        
        //strings
        if (current == '"') {
            return scanString();
        }
        
        //operators and delimiters
        return scanOperatorOrDelimiter();
    }
    
    private Token scanIdentifierOrKeyword() {
        // [a...z]{a...z}*{0...9}*
        // Check against keywords
        return null; 
    }
    
    private Token scanNumber() {
        // ( 0 | [1...9][0...9]* )
        return null;
    }
    
    private void skipWhitespace() {
        while (position < source.length() && 
               Character.isWhitespace(peek())) {
            if (peek() == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            position++;
        }
    }
    
    private char peek() {
        return position < source.length() ? source.charAt(position) : '\0';
    }
    
    private char advance() {
        char c = peek();
        position++;
        column++;
        return c;
    }
    
    private boolean isLetter(char c) {
        return c >= 'a' && c <= 'z';
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}