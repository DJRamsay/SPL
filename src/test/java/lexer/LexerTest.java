package lexer;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.List;

public class LexerTest {
    
    private Lexer lexer;
    
    @Before
    public void setUp() {
        lexer = null;
    }
    
    // ============================================================
    // IDENTIFIER TESTS
    // ============================================================
    
    @Test
    public void testSimpleIdentifier() throws LexerException {
        lexer = new Lexer("abc");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size()); // identifier + EOF
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("abc", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testIdentifierWithNumbers() throws LexerException {
        lexer = new Lexer("var123");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("var123", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testIdentifierSingleLetter() throws LexerException {
        lexer = new Lexer("x");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("x", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testMultipleIdentifiers() throws LexerException {
        lexer = new Lexer("foo bar123 baz");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(4, tokens.size()); // 3 identifiers + EOF
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("foo", tokens.get(0).getLexeme());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("bar123", tokens.get(1).getLexeme());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals("baz", tokens.get(2).getLexeme());
    }

    @Test(expected = LexerException.class)
    public void testIdentifierWithSpecialChar_ThrowsException() throws LexerException {
        lexer = new Lexer("abc_def");
        lexer.tokenize(); // Should throw - underscore not allowed
    }
    
    @Test(expected = LexerException.class)
    public void testIdentifierWithUppercase_ThrowsException() throws LexerException {
        lexer = new Lexer("Abc");
        lexer.tokenize(); // Should throw - only lowercase allowed
    }
    
    // ============================================================
    // KEYWORD TESTS
    // ============================================================
    
    @Test
    public void testKeyword_Glob() throws LexerException {
        lexer = new Lexer("glob");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.GLOB, tokens.get(0).getType());
        assertEquals("glob", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testKeyword_Main() throws LexerException {
        lexer = new Lexer("main");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.MAIN, tokens.get(0).getType());
    }
    
    @Test
    public void testKeyword_While() throws LexerException {
        lexer = new Lexer("while");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.WHILE, tokens.get(0).getType());
    }
    
    @Test
    public void testKeyword_If() throws LexerException {
        lexer = new Lexer("if");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.IF, tokens.get(0).getType());
    }
    
    @Test
    public void testKeyword_Else() throws LexerException {
        lexer = new Lexer("else");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.ELSE, tokens.get(0).getType());
    }
    
    @Test
    public void testAllOperatorKeywords() throws LexerException {
        lexer = new Lexer("neg not eq or and plus minus mult div");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(10, tokens.size()); // 9 operators + EOF
        assertEquals(TokenType.NEG, tokens.get(0).getType());
        assertEquals(TokenType.NOT, tokens.get(1).getType());
        assertEquals(TokenType.EQ, tokens.get(2).getType());
        assertEquals(TokenType.OR, tokens.get(3).getType());
        assertEquals(TokenType.AND, tokens.get(4).getType());
        assertEquals(TokenType.PLUS, tokens.get(5).getType());
        assertEquals(TokenType.MINUS, tokens.get(6).getType());
        assertEquals(TokenType.MULT, tokens.get(7).getType());
        assertEquals(TokenType.DIV, tokens.get(8).getType());
    }
    
    @Test
    public void testKeywordVsIdentifier() throws LexerException {
        // "var" is keyword, "variable" is identifier
        lexer = new Lexer("var variable");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(3, tokens.size());
        assertEquals(TokenType.VAR, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("variable", tokens.get(1).getLexeme());
    }
    
    // ============================================================
    // NUMBER TESTS
    // ============================================================
    
    @Test
    public void testNumber_Zero() throws LexerException {
        lexer = new Lexer("0");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("0", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testNumber_SingleDigit() throws LexerException {
        lexer = new Lexer("5");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("5", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testNumber_MultipleDigits() throws LexerException {
        lexer = new Lexer("12345");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("12345", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testNumber_StartingWithNonZero() throws LexerException {
        lexer = new Lexer("987654321");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("987654321", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testMultipleNumbers() throws LexerException {
        lexer = new Lexer("0 123 456");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(4, tokens.size()); // 3 numbers + EOF
        assertEquals("0", tokens.get(0).getLexeme());
        assertEquals("123", tokens.get(1).getLexeme());
        assertEquals("456", tokens.get(2).getLexeme());
    }
    
    @Test(expected = LexerException.class)
    public void testNumber_LeadingZero_ThrowsException() throws LexerException {
        lexer = new Lexer("0123");
        lexer.tokenize(); // Should throw - leading zeros not allowed
    }
    
    @Test(expected = LexerException.class)
    public void testNumber_MultipleLeadingZeros_ThrowsException() throws LexerException {
        lexer = new Lexer("00");
        lexer.tokenize();
    }
    
    // ============================================================
    // STRING TESTS
    // ============================================================
    
    @Test
    public void testString_Simple() throws LexerException {
        lexer = new Lexer("\"hello\"");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size());
        assertEquals(TokenType.STRING, tokens.get(0).getType());
        assertEquals("hello", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testString_WithNumbers() throws LexerException {
        lexer = new Lexer("\"abc123\"");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.STRING, tokens.get(0).getType());
        assertEquals("abc123", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testString_WithSpaces() throws LexerException {
        lexer = new Lexer("\"hello world\"");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.STRING, tokens.get(0).getType());
        assertEquals("hello world", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testString_Empty() throws LexerException {
        lexer = new Lexer("\"\"");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.STRING, tokens.get(0).getType());
        assertEquals("", tokens.get(0).getLexeme());
    }
    
    @Test
    public void testString_MaxLength() throws LexerException {
        lexer = new Lexer("\"123456789012345\""); // exactly 15 chars
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.STRING, tokens.get(0).getType());
        assertEquals(15, tokens.get(0).getLexeme().length());
    }
    
    @Test(expected = LexerException.class)
    public void testString_TooLong_ThrowsException() throws LexerException {
        lexer = new Lexer("\"1234567890123456\""); // 16 chars - too long!
        lexer.tokenize();
    }
    
    @Test(expected = LexerException.class)
    public void testString_Unterminated_ThrowsException() throws LexerException {
        lexer = new Lexer("\"hello");
        lexer.tokenize();
    }
    
    @Test(expected = LexerException.class)
    public void testString_WithSpecialChars_ThrowsException() throws LexerException {
        lexer = new Lexer("\"hello!\"");
        lexer.tokenize(); // Should throw - only letters/digits/spaces allowed
    }
    
    // ============================================================
    // DELIMITER AND OPERATOR TESTS
    // ============================================================
    
    @Test
    public void testDelimiter_Parentheses() throws LexerException {
        lexer = new Lexer("( )");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(3, tokens.size());
        assertEquals(TokenType.LPAREN, tokens.get(0).getType());
        assertEquals(TokenType.RPAREN, tokens.get(1).getType());
    }
    
    @Test
    public void testDelimiter_Braces() throws LexerException {
        lexer = new Lexer("{ }");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.LBRACE, tokens.get(0).getType());
        assertEquals(TokenType.RBRACE, tokens.get(1).getType());
    }
    
    @Test
    public void testDelimiter_Semicolon() throws LexerException {
        lexer = new Lexer(";");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.SEMICOLON, tokens.get(0).getType());
    }
    
    @Test
    public void testDelimiter_Comma() throws LexerException {
        lexer = new Lexer(",");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.COMMA, tokens.get(0).getType());
    }
    
    @Test
    public void testOperator_Assign() throws LexerException {
        lexer = new Lexer("=");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.ASSIGN, tokens.get(0).getType());
    }
    
    @Test
    public void testOperator_GreaterThan() throws LexerException {
        lexer = new Lexer(">");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(TokenType.GT, tokens.get(0).getType());
    }
    
    @Test(expected = LexerException.class)
    public void testInvalidCharacter_ThrowsException() throws LexerException {
        lexer = new Lexer("@");
        lexer.tokenize();
    }
    
    @Test(expected = LexerException.class)
    public void testInvalidCharacter_Hash_ThrowsException() throws LexerException {
        lexer = new Lexer("#");
        lexer.tokenize();
    }
    
    // ============================================================
    // WHITESPACE TESTS
    // ============================================================
    
    @Test
    public void testWhitespace_Spaces() throws LexerException {
        lexer = new Lexer("abc   def");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(3, tokens.size()); // 2 identifiers + EOF
        assertEquals("abc", tokens.get(0).getLexeme());
        assertEquals("def", tokens.get(1).getLexeme());
    }
    
    @Test
    public void testWhitespace_Tabs() throws LexerException {
        lexer = new Lexer("abc\t\tdef");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(3, tokens.size());
        assertEquals("abc", tokens.get(0).getLexeme());
        assertEquals("def", tokens.get(1).getLexeme());
    }
    
    @Test
    public void testWhitespace_Newlines() throws LexerException {
        lexer = new Lexer("abc\n\ndef");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(3, tokens.size());
        assertEquals("abc", tokens.get(0).getLexeme());
        assertEquals("def", tokens.get(1).getLexeme());
        // Check line numbers
        assertEquals(1, tokens.get(0).getLine());
        assertEquals(3, tokens.get(1).getLine());
    }
    
    @Test
    public void testWhitespace_Mixed() throws LexerException {
        lexer = new Lexer("  \t\n  abc  \n\t  ");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(2, tokens.size()); // identifier + EOF
        assertEquals("abc", tokens.get(0).getLexeme());
    }
    
    // ============================================================
    // COMPLEX/INTEGRATION TESTS
    // ============================================================
    
    @Test
    public void testSimpleAssignment() throws LexerException {
        lexer = new Lexer("x = 5");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(4, tokens.size());
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals(TokenType.ASSIGN, tokens.get(1).getType());
        assertEquals(TokenType.NUMBER, tokens.get(2).getType());
        assertEquals(TokenType.EOF, tokens.get(3).getType());
    }
    
    @Test
    public void testFunctionCall() throws LexerException {
        lexer = new Lexer("foo(x, y)");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(7, tokens.size()); // foo ( x , y ) EOF
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals(TokenType.LPAREN, tokens.get(1).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals(TokenType.COMMA, tokens.get(3).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType());
        assertEquals(TokenType.RPAREN, tokens.get(5).getType());
    }
    
    @Test
    public void testWhileLoop() throws LexerException {
        lexer = new Lexer("while x > 0 { halt }");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(8, tokens.size());
        assertEquals(TokenType.WHILE, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals(TokenType.GT, tokens.get(2).getType());
        assertEquals(TokenType.NUMBER, tokens.get(3).getType());
        assertEquals(TokenType.LBRACE, tokens.get(4).getType());
        assertEquals(TokenType.HALT, tokens.get(5).getType());
        assertEquals(TokenType.RBRACE, tokens.get(6).getType());
    }
    
    @Test
    public void testPrintStatement() throws LexerException {
        lexer = new Lexer("print \"hello world\"");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(3, tokens.size());
        assertEquals(TokenType.PRINT, tokens.get(0).getType());
        assertEquals(TokenType.STRING, tokens.get(1).getType());
        assertEquals("hello world", tokens.get(1).getLexeme());
    }
    
    @Test
    public void testComplexExpression() throws LexerException {
        lexer = new Lexer("( x plus ( y mult 3 ) )");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(10, tokens.size());
        assertEquals(TokenType.LPAREN, tokens.get(0).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals(TokenType.PLUS, tokens.get(2).getType());
        assertEquals(TokenType.LPAREN, tokens.get(3).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType());
        assertEquals(TokenType.MULT, tokens.get(5).getType());
        assertEquals(TokenType.NUMBER, tokens.get(6).getType());
        assertEquals(TokenType.RPAREN, tokens.get(7).getType());
        assertEquals(TokenType.RPAREN, tokens.get(8).getType());
    }
    
    @Test
    public void testMiniProgram() throws LexerException {
        String program = "glob { x } main { var { y } y = 10 ; halt }";
        lexer = new Lexer(program);
        List<Token> tokens = lexer.tokenize();
        
        // Verify we got all expected tokens
        assertTrue(tokens.size() > 0);
        assertEquals(TokenType.GLOB, tokens.get(0).getType());
        assertEquals(TokenType.MAIN, tokens.get(4).getType());
        assertEquals(TokenType.HALT, tokens.get(14).getType());
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType());
    }
    
    // ============================================================
    // LINE AND COLUMN TRACKING TESTS
    // ============================================================
    
    @Test
    public void testLineTracking() throws LexerException {
        lexer = new Lexer("abc\ndef\nghi");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(1, tokens.get(0).getLine()); // abc on line 1
        assertEquals(2, tokens.get(1).getLine()); // def on line 2
        assertEquals(3, tokens.get(2).getLine()); // ghi on line 3
    }
    
    @Test
    public void testColumnTracking() throws LexerException {
        lexer = new Lexer("abc def");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(1, tokens.get(0).getColumn()); // abc starts at column 1
        assertEquals(5, tokens.get(1).getColumn()); // def starts at column 5
    }
    
    // ============================================================
    // EOF TOKEN TEST
    // ============================================================
    
    @Test
    public void testEOFToken_AlwaysPresent() throws LexerException {
        lexer = new Lexer("abc");
        List<Token> tokens = lexer.tokenize();
        
        Token lastToken = tokens.get(tokens.size() - 1);
        assertEquals(TokenType.EOF, lastToken.getType());
    }
    
    @Test
    public void testEOFToken_EmptyInput() throws LexerException {
        lexer = new Lexer("");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).getType());
    }
    
    @Test
    public void testEOFToken_OnlyWhitespace() throws LexerException {
        lexer = new Lexer("   \n\t  ");
        List<Token> tokens = lexer.tokenize();
        
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).getType());
    }
}