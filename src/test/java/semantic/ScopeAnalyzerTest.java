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
    
    /**
     * TEST 13: Procedure with parameters
     */
    @Test
    public void testProcedureWithParameters() {
        List<Token> tokens = createTokens(
            TokenType.GLOB, "glob",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.PROC, "proc",
            TokenType.LBRACE, "{",
            TokenType.IDENTIFIER, "myproc",
            TokenType.LPAREN, "(",
            TokenType.IDENTIFIER, "x",
            TokenType.IDENTIFIER, "y",
            TokenType.RPAREN, ")",
            TokenType.LBRACE, "{",
            TokenType.LOCAL, "local",
            TokenType.LBRACE, "{",
            TokenType.RBRACE, "}",
            TokenType.HALT, "halt",
            TokenType.RBRACE, "}",
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
        
        assertTrue("Procedure with parameters should be valid", result.isSuccess());
        assertTrue("Should have no errors, but found: " + result.getErrors(), result.getErrors().isEmpty());
    }

   /**
     * TEST 14: Variable-Function name conflict (should fail)
     */
@Test
public void testVariableFunctionNameConflict() throws Exception {
    String source = "glob { myfunc ; } proc { } func { myfunc ( ) { local { } ; return 1 } } main { var { } halt }";
    List<Token> tokens = new Lexer(source).tokenize();
    
    // DEBUG: Print all tokens
    System.out.println("=== ALL TOKENS ===");
    for (int i = 0; i < tokens.size(); i++) {
        Token t = tokens.get(i);
        if (t.getType() != TokenType.EOF) {
            System.out.printf("%2d: %-15s '%s' at %d:%d%n", 
                i, t.getType(), t.getLexeme(), t.getLine(), t.getColumn());
        }
    }
    
    ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
    ScopeAnalysisResult result = analyzer.analyze();
    
    // DEBUG: Print symbol table and errors
    analyzer.debugSymbolTable();
    if (!result.isSuccess()) {
        System.out.println("Errors found:");
        for (String error : result.getErrors()) {
            System.out.println("  - " + error);
        }
    } else {
        System.out.println("No errors found - but there should be a naming conflict!");
    }
    
    assertFalse("Variable-function name conflict should fail", result.isSuccess());
    assertFalse("Should have errors due to name conflict", result.getErrors().isEmpty());
}


    /**
     * TEST 15: Procedure-Function name conflict (should fail)
     */
    @Test
    public void testProcedureFunctionNameConflict() throws Exception {
        String source = "glob { } proc { myname ( ) { local { } halt } } func { myname ( ) { local { } ; return 1 } } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertFalse("Procedure-function name conflict should fail", result.isSuccess());
        assertFalse("Should have errors due to name conflict", result.getErrors().isEmpty());
    }

    /**
     * TEST 16: Shadowing parameter with local variable (should fail)
     */
    @Test
    public void testShadowingParameterWithLocal() throws Exception {
        String source = "glob { } proc { myproc ( x ) { local { x ; } halt } } func { } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        
        // DEBUG: Print tokens around the procedure
        System.out.println("=== TOKENS AROUND PROCEDURE ===");
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType() != TokenType.EOF && i >= 5 && i <= 20) {
                System.out.printf("%2d: %-15s '%s' at %d:%d%n", 
                    i, t.getType(), t.getLexeme(), t.getLine(), t.getColumn());
            }
        }
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        if (!result.isSuccess()) {
            System.out.println("Errors found:");
            for (String error : result.getErrors()) {
                System.out.println("  - " + error);
            }
        } else {
            System.out.println("No errors found - but shadowing should be detected!");
            analyzer.debugSymbolTable();
        }
        
        assertFalse("Shadowing parameters should fail", result.isSuccess());
        assertFalse("Should have errors due to shadowing", result.getErrors().isEmpty());
    }

    /**
     * TEST 17: Valid same name in different scopes (should pass)
     */
    @Test
    public void testSameNameInDifferentScopes() throws Exception {
        String source = "glob { x ; } proc { myproc ( ) { local { x ; } x = 1 ; } } func { } main { var { x ; } x = 2 ; halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        // DEBUG: Print symbol table and errors
        analyzer.debugSymbolTable();
        if (!result.isSuccess()) {
            System.out.println("Errors found:");
            for (String error : result.getErrors()) {
                System.out.println("  - " + error);
            }
        }
        
        assertTrue("Same name in different scopes should be allowed", result.isSuccess());
    }

    /**
     * TEST 18: Valid parameter usage
     */
    @Test
    public void testValidParameterUsage() throws Exception {
        String source = "glob { } proc { myproc ( a b ) { local { } a = 1 ; b = 2 ; } } func { } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Parameter usage should be valid", result.isSuccess());
    }

    /**
     * TEST 19: Valid function with return
     */
    @Test
    public void testValidFunctionReturn() throws Exception {
        String source = "glob { } proc { } func { myfunc ( ) { local { } ; return 1 } } main { var { x ; } x = myfunc ( ) ; halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Function with return should be valid", result.isSuccess());
    }

    /**
     * TEST 20: Max three parameters (should pass)
     */
    @Test
    public void testMaxThreeParameters() throws Exception {
        String source = "glob { } proc { myproc ( a b c ) { local { } halt } } func { } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Three parameters should be allowed", result.isSuccess());
    }

    /**
     * TEST 21: Max three local variables (should pass)
     */
    @Test
    public void testMaxThreeLocals() throws Exception {
        String source = "glob { } proc { myproc ( ) { local { x y z ; } halt } } func { } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        
        // DEBUG: Print tokens around the local block
        System.out.println("=== TOKENS AROUND LOCAL BLOCK ===");
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType() != TokenType.EOF && i >= 10 && i <= 20) { // Focus on local block area
                System.out.printf("%2d: %-15s '%s' at %d:%d%n", 
                    i, t.getType(), t.getLexeme(), t.getLine(), t.getColumn());
            }
        }
        
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        if (!result.isSuccess()) {
            System.out.println("Errors found:");
            for (String error : result.getErrors()) {
                System.out.println("  - " + error);
            }
        }
        
        assertTrue("Three local variables should be allowed", result.isSuccess());
    }

    /**
     * TEST 22: Global variable access in procedure (should pass)
     */
    @Test
    public void testGlobalVariableAccessInProcedure() throws Exception {
        String source = "glob { globalVar ; } proc { myproc ( ) { local { } globalVar = 5 ; } } func { } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Global variable access in procedure should be valid", result.isSuccess());
    }

    /**
     * TEST 23: Complex nested if-else
     */
    @Test
    public void testComplexNestedIfElse() throws Exception {
        String source = "glob { x ; } proc { } func { } main { " +
                "var { } " +
                "if ( x > 0 ) { " +
                "  if ( x > 10 ) { " +
                "    print \"large\" " +
                "  } else { " +
                "    print \"medium\" " +
                "  } " +
                "} else { " +
                "  print \"negative\" " +
                "} ; " +
                "halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Complex nested if-else should be valid", result.isSuccess());
    }

    /**
     * TEST 24: Multiple procedure calls
     */
    @Test
    public void testMultipleProcedureCalls() throws Exception {
        String source = "glob { } " +
                "proc { " +
                "  proc1 ( ) { local { } halt } " +
                "  proc2 ( ) { local { } halt } " +
                "} " +
                "func { } " +
                "main { " +
                "  var { } " +
                "  proc1 ( ) ; " +
                "  proc2 ( ) ; " +
                "  halt " +
                "}";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Multiple procedure calls should be valid", result.isSuccess());
    }

    /**
     * TEST 25: Function call in expression
     */
@Test
public void testFunctionCallInExpression() throws Exception {
    String source = "glob { } " +
            "proc { } " +
            "func { " +
            "  add ( a b ) { local { } ; return ( a plus b ) } " +
            "} " +
            "main { " +
            "  var { result ; } " +
            "  result = add ( 5 3 ) ; " +
            "  halt " +
            "}";
    List<Token> tokens = new Lexer(source).tokenize();
    
    // DEBUG: Print tokens around the assignment
    System.out.println("=== TOKENS AROUND ASSIGNMENT ===");
    for (int i = 0; i < tokens.size(); i++) {
        Token t = tokens.get(i);
        if (t.getType() != TokenType.EOF && i >= 25 && i <= 35) { // Focus on assignment area
            System.out.printf("%2d: %-15s '%s' at %d:%d%n", 
                i, t.getType(), t.getLexeme(), t.getLine(), t.getColumn());
        }
    }
    
    ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
    ScopeAnalysisResult result = analyzer.analyze();
    
    // DEBUG: Print symbol table and errors
    analyzer.debugSymbolTable();
    if (!result.isSuccess()) {
        System.out.println("Errors found:");
        for (String error : result.getErrors()) {
            System.out.println("  - " + error);
        }
    }
    
    assertTrue("Function call in expression should be valid", result.isSuccess());
}

    @Test
    public void testSimpleFunctionDefinition() throws Exception {
        String source = "glob { } " +
                "proc { } " +
                "func { " +
                "  simple ( ) { local { } ; return 1 } " +  // Simple function
                "} " +
                "main { " +
                "  var { } " +
                "  halt " +
                "}";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Simple function definition should work", result.isSuccess());
    }

    @Test
    public void testFunctionCallWithNoParameters() throws Exception {
        String source = "glob { } " +
                "proc { } " +
                "func { " +
                "  getValue ( ) { local { } ; return 42 } " +
                "} " +
                "main { " +
                "  var { result ; } " +
                "  result = getValue ( ) ; " +
                "  halt " +
                "}";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();

        assertTrue("Function call with no parameters should work", result.isSuccess());
    }

    /**
     * TEST 26: Empty everything except main
     */
    @Test
    public void testEmptyEverythingExceptMain() throws Exception {
        String source = "glob { } proc { } func { } main { var { } halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Empty everything except main should be valid", result.isSuccess());
    }

    /**
     * TEST 27: Complex arithmetic expression
     */
    @Test
    public void testComplexArithmeticExpression() throws Exception {
        String source = "glob { a b c ; } proc { } func { } main { " +
                "var { result ; } " +
                "result = ( ( a plus b ) mult ( c minus 1 ) ) ; " +
                "halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Complex arithmetic expression should be valid", result.isSuccess());
    }


    /**
     * TEST 29: Unary operators
     */
    @Test
    public void testUnaryOperators() throws Exception {
        String source = "glob { x ; } proc { } func { } main { " +
                "var { result ; } " +
                "result = ( neg x ) ; " +
                "if ( not ( x > 0 ) ) { " +
                "  result = 0 " +
                "} ; " +
                "halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Unary operators should be valid", result.isSuccess());
    }

    /**
     * TEST 30: Mixed variable declarations (with and without 'var' keyword)
     */
    @Test
    public void testMixedVariableDeclarations() throws Exception {
        String source = "glob { var x ; y z ; } proc { } func { } main { " +
                "var { var a ; b ; } " +
                "halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Mixed variable declarations should be valid", result.isSuccess());
    }


    /**
     * TEST 34: Do-until with multiple statements
     */
    @Test
    public void testDoUntilWithMultipleStatements() throws Exception{
        String source = "glob { x ; } proc { } func { } main { " +
                "var { } " +
                "do { " +
                "  x = ( x plus 1 ) ; " +
                "  print x ; " +
                "  if ( x > 50 ) { " +
                "    print \"over 50\" " +
                "  } " +
                "} until ( x > 100 ) ; " +
                "halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Do-until with multiple statements should be valid", result.isSuccess());
    }

    /**
     * TEST 35: Print with variables and strings
     */
    @Test
    public void testPrintWithVariablesAndStrings() throws Exception {
        String source = "glob { name age ; } proc { } func { } main { " +
                "var { } " +
                "print \"Name \" ; " +
                "print name ; " +
                "print \"Age \" ; " +
                "print age ; " +
                "halt }";
        List<Token> tokens = new Lexer(source).tokenize();
        ScopeAnalyzer analyzer = new ScopeAnalyzer(tokens);
        ScopeAnalysisResult result = analyzer.analyze();
        
        assertTrue("Print with variables and strings should be valid", result.isSuccess());
    }

}