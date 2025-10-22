import java.util.List;
import lexer.Lexer;
import lexer.Token;
import parser.Parser;
import parser.ParserException;

/**
 * Real SPL Program Test Suite
 * Tests complete, realistic SPL programs to validate Lexer + Parser integration
 */
public class parserTests {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("     SPL REAL PROGRAM VALIDATION TESTS                  ");
        System.out.println("=========================================================\n");
        
        testMinimalProgram();
        testSimpleCalculator();
        testCounterProgram();
        testFactorialProgram();
        testMaxFinder();
        testLoopExample();
        testNestedControlFlow();
        testVocabularyRules();
        
        printSummary();
    }

    private static void testProgram(String name, String code, boolean shouldPass) {
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
            } else {
                System.out.println("[RESULT] ✗ FAIL - Expected syntax error but passed");
                failedTests++;
            }
            
        } catch (Exception e) {
            if (!shouldPass) {
                System.out.println("[RESULT] ✓ PASS - Correctly rejected invalid program");
                System.out.println("[ERROR] " + e.getMessage());
                passedTests++;
            } else {
                System.out.println("[RESULT] ✗ FAIL - Valid program rejected");
                System.out.println("[ERROR] " + e.getMessage());
                failedTests++;
            }
        }
    }

    // Format code for readable output
    private static String formatCode(String code) {
        return code.replace(" glob ", "\nglob ")
                   .replace(" proc ", "\nproc ")
                   .replace(" func ", "\nfunc ")
                   .replace(" main ", "\nmain ");
    }

    // =================================================================
    // TEST 1: Minimal Valid Program
    // =================================================================
    private static void testMinimalProgram() {
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
    private static void testSimpleCalculator() {
        String code = 
            "glob { var a ; var b ; var result ; } " +
            "proc { } " +
            "func { add (x y) { local { var sum ; } sum = (x plus y) ; return sum } } " +
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
    private static void testCounterProgram() {
        String code = 
            "glob { var counter ; } " +
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
    private static void testFactorialProgram() {
        String code = 
            "glob { var n ; var result ; } " +
            "proc { } " +
            "func { " +
            "  factorial (num) { " +
            "    local { var fact ; var i ; } " +
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
    private static void testMaxFinder() {
        String code = 
            "glob { var x ; var y ; var z ; var maximum ; } " +
            "proc { } " +
            "func { " +
            "  max3 (a b c) { " +
            "    local { var temp ; } " +
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
    private static void testLoopExample() {
        String code = 
            "glob { var count ; } " +
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
    private static void testNestedControlFlow() {
        String code = 
            "glob { var num ; } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { var i ; } " +
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
    // TEST 8: Vocabulary Rule Validation
    // =================================================================
    private static void testVocabularyRules() {
        System.out.println("\n=========================================================");
        System.out.println(" VOCABULARY RULE VALIDATION                              ");
        System.out.println("=========================================================");
        
        // Valid identifiers (Rule 2: [a-z][a-z]*[0-9]*)
        String validIds = 
            "glob { var abc ; var x123 ; var temp1 ; } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        testProgram("Valid Identifiers", validIds, true);
        
        // Invalid: starts with digit
        String invalidId1 = 
            "glob { var 1x ; } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        testProgram("Invalid Identifier (starts with digit)", invalidId1, false);
        
        // Invalid: keyword as identifier
        String invalidId2 = 
            "glob { var while ; } " +
            "proc { } " +
            "func { } " +
            "main { var { } halt }";
        testProgram("Invalid Identifier (keyword)", invalidId2, false);
        
        // Valid numbers (Rule 3: 0 | [1-9][0-9]*)
        String validNums = 
            "glob { var x ; } " +
            "proc { } " +
            "func { } " +
            "main { var { } x = 0 ; x = 42 ; x = 999 ; halt }";
        testProgram("Valid Numbers", validNums, true);
        
        // Invalid: leading zero
        String invalidNum = 
            "glob { var x ; } " +
            "proc { } " +
            "func { } " +
            "main { var { } x = 007 ; halt }";
        testProgram("Invalid Number (leading zero)", invalidNum, false);
        
        // String tests (Rule 4: max 15 chars)
        String validString = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { var { } print \"hello world\" ; halt }";
        testProgram("Valid String (within 15 chars)", validString, true);
        
        String maxString = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { var { } print \"123456789012345\" ; halt }";
        testProgram("Valid String (exactly 15 chars)", maxString, true);
        
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