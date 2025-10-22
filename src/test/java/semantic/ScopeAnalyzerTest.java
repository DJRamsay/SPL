package semantic;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import semantic.ScopeAnalyzer.ScopeAnalysisResult;

public class ScopeAnalyzerTest {

    private List<Token> createTokens(Object... tokenData) {
        List<Token> tokens = new ArrayList<>();
        
        for (int i = 0; i < tokenData.length; i += 2) {
            TokenType type = (TokenType) tokenData[i];
            String lexeme = (String) tokenData[i + 1];
            tokens.add(new Token(type, lexeme, 1, 1));
        }
        
        return tokens;
    }
 
    private List<Token> createMinimalProgram() {
        return createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
    }
    
    @Test
    public void testMinimalValidProgram() {
        List<Token> tokens = createMinimalProgram();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        
        ScopeAnalysisResult result = analyzer.analyze();

        if (!result.getErrors().isEmpty()) {
            System.out.println("Errors found:");
            for (String error : result.getErrors()) {
                System.out.println("  - " + error);
            }
        }

        assertTrue( "Minimal program should be valid", result.isSuccess());
        assertTrue("Should have no errors, but found: " + result.getErrors(), result.getErrors().isEmpty());        
    }
    
    /**
     * TEST 2: Program with global variables
     */
    @Test
    public void testProgramWithGlobalVariables() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "x",
            TokenType.SEMICOLON, ";",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "y",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        // assertTrue("Program with global variables should be valid", result.isSuccess());
        assertTrue("Should have no errors, but found: " + result.getErrors(), result.getErrors().isEmpty());        

    }
    
    /**
     * TEST 3: Program with variable usage in main
     */
    @Test
    public void testVariableUsageInMain() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.IDENTIFIER, "num",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",

            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            // num = 5;
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.IDENTIFIER, "num",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Using declared global variable should work", result.isSuccess());
        assertTrue("Should have no errors, but found: " + result.getErrors(), result.getErrors().isEmpty());        

    }
    
    /**
     * TEST 4: While loop with valid condition
     */
    @Test
    public void testWhileLoopWithCondition() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "i",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            // while (i > 10) { halt }
            TokenType.WHILE, "while",
            TokenType.LPAREN, "(",
            TokenType.IDENTIFIER, "i",
            TokenType.GT, ">",
            TokenType.NUMBER, "10",
            TokenType.RPAREN, ")",
            TokenType.LBRACE, "{",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("While loop with condition should be valid", result.isSuccess());
        assertTrue("Should have no errors, but found: " + result.getErrors(), result.getErrors().isEmpty());        

    }
    
    /**
     * TEST 5: If-else statement
     */
    @Test
    public void testIfElseStatement() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "x",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            // if (x > 5) { halt } else { halt }
            TokenType.IF, "if",
            TokenType.LPAREN, "(",
            TokenType.IDENTIFIER, "x",
            TokenType.GT, ">",
            TokenType.NUMBER, "5",
            TokenType.RPAREN, ")",
            TokenType.LBRACE, "{",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.ELSE, "else",
            TokenType.LBRACE, "{",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("If-else statement should be valid", result.isSuccess());
        assertTrue("Should have no errors, but found: " + result.getErrors(), result.getErrors().isEmpty());        

    }
    
    // =========================================================================
    // TEST CATEGORY 2: Error Detection (Should FAIL with specific errors)
    // =========================================================================
    
    /**
     * TEST 6: Using undeclared variable
     * This should catch a scope error!
     */
    @Test
    public void testUndeclaredVariable() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            // Using 'x' without declaring it
            TokenType.IDENTIFIER, "x",
            TokenType.ASSIGN, "=",
            TokenType.NUMBER, "5",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        // We EXPECT this to fail!
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
        
        // Check that the error message mentions the undeclared variable
        String errorMessage = result.getErrors().get(0);
        assertTrue(errorMessage.contains("Undeclared") || errorMessage.contains("x"));
    }
    
    /**
     * TEST 7: Duplicate variable declaration in same scope
     */
    @Test
    public void testDuplicateVariableInSameScope() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "x",
            TokenType.SEMICOLON, ";",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "x",  // Duplicate!
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }
    
    /**
     * TEST 8: Missing required sections
     */
    @Test
    public void testMissingMainSection() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            // Missing main!
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertFalse(result.isSuccess());
    }
    
    // =========================================================================
    // TEST CATEGORY 3: Complex Scenarios
    // =========================================================================
    
    /**
     * TEST 9: Nested scopes (local variables)
     */
    @Test
    public void testNestedScopes() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "global",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            // Local scope
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "local",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            // Using both variables
            TokenType.IDENTIFIER, "global",
            TokenType.ASSIGN, "=",
            TokenType.NUMBER, "1",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue(result.isSuccess());
    }
    
    /**
     * TEST 10: Print statement
     */
    @Test
    public void testPrintStatement() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.PRINT, "print",
            TokenType.STRING, "Hello World",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue(result.isSuccess());
    }
    
    /**
     * TEST 11: Complex expression with operators
     */
    @Test
    public void testComplexExpression() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "a",
            TokenType.SEMICOLON, ";",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "b",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            // a = (a plus b);
            TokenType.IDENTIFIER, "a",
            TokenType.ASSIGN, "=",
            TokenType.LPAREN, "(",
            TokenType.IDENTIFIER, "a",
            TokenType.PLUS, "plus",
            TokenType.IDENTIFIER, "b",
            TokenType.RPAREN, ")",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue(result.isSuccess());
    }
    
    /**
     * TEST 12: Do-until loop
     */
    @Test
    public void testDoUntilLoop() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.IDENTIFIER, "i",
            TokenType.SEMICOLON, ";",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.FUNC, "func",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.MAIN, "main",
            TokenType.LBRACE, "{",
            TokenType.VAR, "var",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            // do { halt } until (i > 5)
            TokenType.DO, "do",
            TokenType.LBRACE, "{",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.UNTIL, "until",
            TokenType.LPAREN, "(",
            TokenType.IDENTIFIER, "i",
            TokenType.GT, ">",
            TokenType.NUMBER, "5",
            TokenType.RPAREN, ")",
            TokenType.SEMICOLON, ";",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
            TokenType.EOF, ""
        );
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue(result.isSuccess());
    }

    @Test
    public void testProcedureCall() throws Exception {
        String source = "glob { } proc { myproc ( ) { local { } halt } } func { } main { var { } myproc ( ) ; halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalyzer.ScopeAnalysisResult result = analyzer.analyze();

        assertTrue("Procedure call should be valid", result.isSuccess());
    }
}