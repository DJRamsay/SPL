package lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lexer.LexerException;

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

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("glob", TokenType.GLOB);
        KEYWORDS.put("proc", TokenType.PROC);
        KEYWORDS.put("func", TokenType.FUNC);
        KEYWORDS.put("main", TokenType.MAIN);
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("local", TokenType.LOCAL);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("halt", TokenType.HALT);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("do", TokenType.DO);
        KEYWORDS.put("until", TokenType.UNTIL);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("neg", TokenType.NEG);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("eq", TokenType.EQ);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("plus", TokenType.PLUS);
        KEYWORDS.put("minus", TokenType.MINUS);
        KEYWORDS.put("mult", TokenType.MULT);
        KEYWORDS.put("div", TokenType.DIV);
    }
    
    private Token scanIdentifierOrKeyword() throws LexerException {
        // [a...z]{a...z}*{0...9}*
        // Check against keywords
        int startLine = line;
        int startColumn = column;
        StringBuilder lexeme = new StringBuilder();
        
        // First character must be a lowercase
        if (!isLetter(peek())) {
            throw new LexerException(
                String.format("Invalid identifier at line %d, column %d: expected letter", 
                    line, column)
            );
        }
        
        lexeme.append(advance());
        
        while (isLetter(peek())) {
            lexeme.append(advance());
        }
        
        // Then optional digits
        while (isDigit(peek())) {
            lexeme.append(advance());
        }
        
        String text = lexeme.toString();
        
        // Check if it's a keyword
        TokenType type = KEYWORDS.get(text);
        if (type != null) {
            return new Token(type, text, startLine, startColumn);
        }
        
        // It's a user-defined identifier
        return new Token(TokenType.IDENTIFIER, text, startLine, startColumn);
    }
    
    private Token scanNumber() throws LexerException{
        // ( 0 | [1...9][0...9]* )
        int startLine = line;
        int startColumn = column;
        StringBuilder lexeme = new StringBuilder();

        char first = peek();
        
        if (first == '0'){  //Must be standalone 0
            lexeme.append(advance());

            if (isDigit(peek())){ //0 cant be followed by more numbers
                throw new LexerException(
                    String.format("Invalid number at line %d, column %d: leading zero not allowed",
                        line, column)  
                );
            }
        } else if (first >= '1' && first <= '9'){
            lexeme.append(advance());

            while(isDigit(peek())){
                lexeme.append(advance());
            }
        } else {
            throw new LexerException(
                String.format("Invalid number at line %d, column %d: Expected digit",
                    line, column)
            );
        }
        return new Token(TokenType.NUMBER, lexeme.toString(), startLine, startColumn);
    }

    private Token scanString() throws LexerException{
        // "foo"
        int startLine = line;
        int startColumn = column;

        if (peek() != '"'){
            throw new LexerException(
                String.format("Expected opening quote at line %d, column %d",
                    line, column)
            );
        } 

        advance(); //skip opening "

        StringBuilder lexeme = new StringBuilder();

        while(peek() != '"'  && peek() != '\0'){
            char c = peek();

            if (!isLetter(c) && !isDigit(c) && !Character.isWhitespace(c)) {
                throw new LexerException(
                    String.format("Invalid character '%c' in string at line %d, column %d: " +
                        "only letters, digits, and spaces allowed",
                        c, line, column)
                );
            }
            
            lexeme.append(advance());

            if (lexeme.length() > 15) {
                throw new LexerException(
                    String.format("String exceeds maximum length of 15 at line %d, column %d",
                        startLine, startColumn)
                );
            }
        } 

        if (peek() != '"') {
            throw new LexerException(
                String.format("Unterminated string starting at line %d, column %d",
                    startLine, startColumn)
            );
        }
        
        advance(); // skip closing "
        
        return new Token(TokenType.STRING, lexeme.toString(), startLine, startColumn);
    }

    private Token scanOperatorOrDelimiter() throws LexerException{
        // <>+-
        int startLine = line;
        int startColumn = column;
        char c = advance();

        switch (c) {
            case '(':
                return new Token(TokenType.LPAREN, "(", startLine, startColumn);
            case ')':
                return new Token(TokenType.RPAREN, ")", startLine, startColumn);
            case '{':
                return new Token(TokenType.LBRACE, "{", startLine, startColumn);
            case '}':
                return new Token(TokenType.RBRACE, "}", startLine, startColumn);
            case ';':
                return new Token(TokenType.SEMICOLON, ";", startLine, startColumn);
            case ',':
                return new Token(TokenType.COMMA, ",", startLine, startColumn);
            case '=':
                return new Token(TokenType.ASSIGN, "=", startLine, startColumn);
            case '>':
                return new Token(TokenType.GT, ">", startLine, startColumn);
            default:
                throw new LexerException(
                    String.format("Unexpected character '%c' at line %d, column %d",
                        c, startLine, startColumn)
                );
        }
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