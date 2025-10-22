package parser;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.*;

import java.util.List;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ParserException;

/**
 * Real SPL Program Test Suite
 * Tests complete, realistic SPL programs to validate Lexer + Parser integration
 */
public class ParserTests {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    @BeforeClass
    public static void setUp() {
        System.out.println("=========================================================");
        System.out.println("     SPL REAL PROGRAM VALIDATION TESTS                  ");
        System.out.println("=========================================================\n");
    }

    @AfterClass
    public static void tearDown() {
        printSummary();
    }

    private void testProgram(String name, String code, boolean shouldPass) {
        totalTests++;
        System.out.println("\n---------------------------------------------------------");
        System.out.println("TEST: " + name);
        System.out.println("---------------------------------------------------------");
        System.out.println("Code:\n" + formatCode(code));
        System.out.println();
        
        try {
            // Lexical Analysis
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.tokenize();
            System.out.println("[LEXER] Generated " + tokens.size() + " tokens");
            
            // Syntax Analysis
            Parser parser = new Parser(tokens);
            parser.parse();
            System.out.println("[PARSER] Parsing completed successfully");
            
            if (shouldPass) {
                System.out.println("[RESULT] ✓ PASS - Program is valid");
                passedTests++;
                assertTrue("Program should be valid", true);
            } else {
                System.out.println("[RESULT] ✗ FAIL - Expected syntax error but passed");
                failedTests++;
                fail("Expected syntax error but parsing succeeded");
            }
            
        } catch (Exception e) {
            if (!shouldPass) {
                System.out.println("[RESULT] ✓ PASS - Correctly rejected invalid program");
                System.out.println("[ERROR] " + e.getMessage());
                passedTests++;
                assertTrue("Correctly rejected invalid program", true);
            } else {
                System.out.println("[RESULT] ✗ FAIL - Valid program rejected");
                System.out.println("[ERROR] " + e.getMessage());
                failedTests++;
                fail("Valid program was rejected: " + e.getMessage());
            }
        }
    }

    // Format code for readable output
    private String formatCode(String code) {
        return code.replace(" glob ", "\nglob ")
                   .replace(" proc ", "\nproc ")
                   .replace(" func ", "\nfunc ")
                   .replace(" main ", "\nmain ");
    }

    // =================================================================
    // TEST 1: Minimal Valid Program
    // =================================================================
    @Test
    public void testMinimalProgram() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        
        testProgram("Minimal Valid Program", code, true);
    }

    // =================================================================
    // TEST 2: Simple Calculator (Add Two Numbers)
    // =================================================================
    //TODO: Fix unit test
    @Test
    public void testSimpleCalculator() {
        String code = 
            "glob { a b result } " +
            "proc { } " +
            "func { add (x y) { local { sum } sum = (x plus y) ; return sum } } " +
            "main { " +
            "  var { } " +
            "  a = 5 ; " +
            "  b = 3 ; " +
            "  result = add (a b) ; " +
            "  print result ; " +
            "  halt " +
            "}";
        
        testProgram("Simple Calculator", code, true);
    }

    // =================================================================
    // TEST 3: Counter with Procedure
    // =================================================================
    @Test
    public void testCounterProgram() {
        String code = 
            "glob { counter } " +
            "proc { " +
            "  increment () { local { } counter = (counter plus 1) } " +
            "  reset () { local { } counter = 0 } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  reset () ; " +
            "  increment () ; " +
            "  increment () ; " +
            "  increment () ; " +
            "  print counter ; " +
            "  halt " +
            "}";
        
        testProgram("Counter Program with Procedures", code, true);
    }

    // =================================================================
    // TEST 4: Factorial Calculator
    // =================================================================
    @Test
    public void testFactorialProgram() {
        String code = 
            "glob { n result } " +
            "proc { } " +
            "func { " +
            "  factorial (num) { " +
            "    local { fact i } " +
            "    fact = 1 ; " +
            "    i = 1 ; " +
            "    while ((i > n) or (i eq n)) { " +
            "      fact = (fact mult i) ; " +
            "      i = (i plus 1) " +
            "    } ; " +
            "    return fact " +
            "  } " +
            "} " +
            "main { " +
            "  var { } " +
            "  n = 5 ; " +
            "  result = factorial (n) ; " +
            "  print result ; " +
            "  halt " +
            "}";
        
        testProgram("Factorial Calculator", code, true);
    }

    // =================================================================
    // TEST 5: Find Maximum of Three Numbers
    // =================================================================
    @Test
    public void testMaxFinder() {
        String code = 
            "glob { x y z maximum } " +
            "proc { } " +
            "func { " +
            "  max3 (a b c) { " +
            "    local { temp } " +
            "    temp = a ; " +
            "    if (b > temp) { " +
            "      temp = b " +
            "    } ; " +
            "    if (c > temp) { " +
            "      temp = c " +
            "    } ; " +
            "    return temp " +
            "  } " +
            "} " +
            "main { " +
            "  var { } " +
            "  x = 10 ; " +
            "  y = 25 ; " +
            "  z = 15 ; " +
            "  maximum = max3 (x y z) ; " +
            "  print maximum ; " +
            "  halt " +
            "}";
        
        testProgram("Find Maximum of Three Numbers", code, true);
    }

    // =================================================================
    // TEST 6: Loop Examples (While and Do-Until)
    // =================================================================
    @Test
    public void testLoopExample() {
        String code = 
            "glob { count } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  count = 0 ; " +
            "  while (count > 5) { " +
            "    print count ; " +
            "    count = (count plus 1) " +
            "  } ; " +
            "  count = 10 ; " +
            "  do { " +
            "    print count ; " +
            "    count = (count minus 1) " +
            "  } until (count eq 0) ; " +
            "  halt " +
            "}";
        
        testProgram("Loop Examples (While and Do-Until)", code, true);
    }

    // =================================================================
    // TEST 7: Nested Control Flow
    // =================================================================
    @Test
    public void testNestedControlFlow() {
        String code = 
            "glob { num } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { i } " +
            "  i = 1 ; " +
            "  while (i > 10) { " +
            "    if ((i div 2) eq 0) { " +
            "      print \"even\" " +
            "    } else { " +
            "      if (i > 5) { " +
            "        print \"odd and big\" " +
            "      } else { " +
            "        print \"odd and small\" " +
            "      } " +
            "    } ; " +
            "    i = (i plus 1) " +
            "  } ; " +
            "  halt " +
            "}";
        
        testProgram("Nested Control Flow", code, true);
    }

    // =================================================================
    // TEST 8: Vocabulary Rule Validation - Valid Identifiers
    // =================================================================
    @Test
    public void testValidIdentifiers() {
        String validIds = 
            "glob { abc x123 temp1 } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        testProgram("Valid Identifiers", validIds, true);
    }
    
    @Test
    public void testInvalidIdentifierStartsWithDigit() {
        String invalidId1 = 
            "glob { 1x } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        testProgram("Invalid Identifier (starts with digit)", invalidId1, false);
    }
    
    @Test
    public void testInvalidIdentifierKeyword() {
        String invalidId2 = 
            "glob { while } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        testProgram("Invalid Identifier (keyword)", invalidId2, false);
    }
    
    @Test
    public void testValidNumbers() {
        String validNums = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { var { } x = 0 ; x = 42 ; x = 999 ; halt }";
        testProgram("Valid Numbers", validNums, true);
    }
    
    @Test
    public void testInvalidNumberLeadingZero() {
        String invalidNum = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { var { } x = 007 ; halt }";
        testProgram("Invalid Number (leading zero)", invalidNum, false);
    }
    
    @Test
    public void testValidStringWithin15Chars() {
        String validString = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { var { } print \"hello world\" ; halt }";
        testProgram("Valid String (within 15 chars)", validString, true);
    }
    
    @Test
    public void testValidStringExactly15Chars() {
        String maxString = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { var { } print \"123456789012345\" ; halt }";
        testProgram("Valid String (exactly 15 chars)", maxString, true);
    }
    
    @Test
    public void testInvalidStringTooLong() {
        String tooLongString = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { var { } print \"1234567890123456\" ; halt }";
        testProgram("Invalid String (16 chars, too long)", tooLongString, false);
    }

    // =================================================================
    // Summary
    // =================================================================
    private static void printSummary() {
        System.out.println("\n=========================================================");
        System.out.println("                  TEST SUMMARY                           ");
        System.out.println("=========================================================");
        System.out.println();
        System.out.println("  Total Tests:    " + totalTests);
        System.out.println("  Passed:         " + passedTests + " [PASS]");
        System.out.println("  Failed:         " + failedTests + " [FAIL]");
        System.out.println();
        
        double successRate = totalTests > 0 ? (100.0 * passedTests / totalTests) : 0;
        System.out.printf("  Success Rate:   %.1f%%\n", successRate);
        System.out.println();
        
        if (failedTests == 0) {
            System.out.println("  *** ALL TESTS PASSED! ***");
            System.out.println("  Your Lexer and Parser are working correctly!");
            System.out.println("  Ready to proceed to Semantic Analysis.");
        } else {
            System.out.println("  *** Some tests failed. Review output above.");
        }
        System.out.println();
    }
}