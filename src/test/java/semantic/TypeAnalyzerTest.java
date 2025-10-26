package semantic;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import lexer.Lexer;
import lexer.Token;
import semantic.TypeAnalyzer.TypeAnalysisResult;
import java.util.List;

public class TypeAnalyzerTest {
    
    private TypeAnalyzer analyzer;
    
    private TypeAnalyzer createAnalyzer(String code) throws Exception {
        Lexer lexer = new Lexer(code);
        List<Token> tokens = lexer.tokenize();
        return new TypeAnalyzer(tokens);
    }
    
    private void assertTypeCheckPasses(String testName, String code) {
        try {
            TypeAnalyzer analyzer = createAnalyzer(code);
            TypeAnalysisResult result = analyzer.analyze();
            
            if (!result.isSuccess()) {
                fail(testName + " - Expected type check to pass, but got errors:\n" +
                     String.join("\n", result.getErrors()));
            }
        } catch (Exception e) {
            fail(testName + " - Exception during test setup: " + e.getMessage());
        }
    }
    

    private void assertTypeCheckFails(String testName, String code, String expectedErrorFragment) {
        try {
            TypeAnalyzer analyzer = createAnalyzer(code);
            TypeAnalysisResult result = analyzer.analyze();
            
            if (result.isSuccess()) {
                fail(testName + " - Expected type check to fail, but it passed");
            }
            
            boolean foundExpectedError = result.getErrors().stream()
                .anyMatch(error -> error.contains(expectedErrorFragment));
            
            if (!foundExpectedError) {
                fail(testName + " - Expected error containing '" + expectedErrorFragment + 
                     "', but got:\n" + String.join("\n", result.getErrors()));
            }
        } catch (Exception e) {
            fail(testName + " - Exception during test setup: " + e.getMessage());
        }
    }
    

    // Test Category 1: Valid Programs (Should PASS)
    @Test
    public void testSimpleNumericAssignment() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  x = 5 ; " +
            "  y = (x plus 10) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Simple Numeric Assignment", code);
    }
    
    @Test
    public void testBooleanConditionsInLoops() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if (x > y) { " +
            "    x = 1 " +
            "  } ; " +
            "  while ((x > 0) and (y > 0)) { " +
            "    x = (x minus 1) " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Boolean Conditions in Loops", code);
    }
    
    @Test
    public void testFunctionWithCorrectReturnType() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { " +
            "  max (a b) { " +
            "    local { temp } " +
            "    if (a > b) { " +
            "      temp = a " +
            "    } else { " +
            "      temp = b " +
            "    } ; " +
            "    return temp " +
            "  } " +
            "} " +
            "main { " +
            "  var { x y result } " +
            "  x = 10 ; " +
            "  y = 20 ; " +
            "  result = max (x y) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Function with Correct Return Type", code);
    }
    
    @Test
    public void testUnaryOperators() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { y z } " +
            "  y = (neg x) ; " +
            "  if (not (x > 0)) { " +
            "    z = 1 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Unary Operators", code);
    }
    
    @Test
    public void testComplexNestedExpressions() {
        String code = 
            "glob { a b c } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { result } " +
            "  if (((a > b) and (b > c)) or (a eq c)) { " +
            "    result = ((a plus b) mult (c minus 1)) " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Complex Nested Expressions", code);
    }
    
    @Test
    public void testProcedureCall() {
        String code = 
            "glob { x } " +
            "proc { " +
            "  setX (val) { " +
            "    local { } " +
            "    x = val " +
            "  } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  setX (42) ; " +
            "  print x ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Procedure Call", code);
    }
    
    @Test
    public void testDoUntilLoop() {
        String code = 
            "glob { counter } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  counter = 0 ; " +
            "  do { " +
            "    counter = (counter plus 1) ; " +
            "    print counter " +
            "  } until (counter eq 10) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Do-Until Loop", code);
    }
    
    @Test
    public void testAllArithmeticOperators() {
        String code = 
            "glob { a b c d e } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  a = (b plus c) ; " +
            "  a = (b minus c) ; " +
            "  a = (b mult c) ; " +
            "  a = (b div c) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("All Arithmetic Operators", code);
    }
    
    @Test
    public void testAllComparisonOperators() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if (x > y) { " +
            "    x = 1 " +
            "  } ; " +
            "  if (x eq y) { " +
            "    x = 2 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("All Comparison Operators", code);
    }
    
    @Test
    public void testAllLogicalOperators() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if ((x > 0) and (y > 0)) { " +
            "    x = 1 " +
            "  } ; " +
            "  if ((x > 0) or (y > 0)) { " +
            "    x = 2 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("All Logical Operators", code);
    }
    
    @Test
    public void testPrintNumericAndString() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  print \"hello\" ; " +
            "  print x ; " +
            "  print 42 ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Print Numeric and String", code);
    }
    
    @Test
    public void testLocalVariablesInProcedure() {
        String code = 
            "glob { } " +
            "proc { " +
            "  calculate (x y) { " +
            "    local { temp result } " +
            "    temp = (x plus y) ; " +
            "    result = (temp mult 2) ; " +
            "    print result " +
            "  } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  calculate (5 10) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Local Variables in Procedure", code);
    }
    
    @Test
    public void testMultipleParameters() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { " +
            "  add3 (a b c) { " +
            "    local { sum } " +
            "    sum = ((a plus b) plus c) ; " +
            "    return sum " +
            "  } " +
            "} " +
            "main { " +
            "  var { result } " +
            "  result = add3 (1 2 3) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Multiple Parameters", code);
    }
    
    @Test
    public void testNestedIfElse() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if (x > y) { " +
            "    if (x > 10) { " +
            "      print \"big\" " +
            "    } else { " +
            "      print \"medium\" " +
            "    } " +
            "  } else { " +
            "    print \"small\" " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Nested If-Else", code);
    }
    

    // Test Category 2: Type Errors (Should FAIL)
    
    @Test
    public void testNumericInBooleanContext_IfCondition() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if (x plus y) { " +
            "    x = 0 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Numeric in If Condition", code, "If condition must be boolean");
    }
    
    @Test
    public void testNumericInBooleanContext_WhileCondition() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  while (x mult y) { " +
            "    x = 0 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Numeric in While Condition", code, "While condition must be boolean");
    }
    
    @Test
    public void testNumericInBooleanContext_UntilCondition() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  do { " +
            "    x = 0 " +
            "  } until (x minus 5) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Numeric in Until Condition", code, "Until condition must be boolean");
    }
    
    @Test
    public void testBooleanInNumericContext_Assignment() {
        String code = 
            "glob { x y result } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  result = ((x > 0) or (y > 0)) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Boolean in Assignment", code, "Assignment requires numeric type");
    }
    
    @Test
    public void testUnaryOperatorTypeMismatch_NotOnNumeric() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { y } " +
            "  y = (not x) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("NOT on Numeric", code, "Type mismatch");
    }
    
    @Test
    public void testUnaryOperatorTypeMismatch_NegOnBoolean() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { result } " +
            "  result = (neg (x > y)) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("NEG on Boolean", code, "Type mismatch");
    }
    
    @Test
    public void testBinaryOperatorTypeMismatch_PlusOnBoolean() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { result } " +
            "  result = ((x > 0) plus (y > 0)) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("PLUS on Boolean", code, "Numeric operator requires numeric operands");
    }
    
    @Test
    public void testBinaryOperatorTypeMismatch_AndOnNumeric() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if ((x and y)) { " +
            "    x = 0 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("AND on Numeric", code, "Boolean operator requires boolean operands");
    }
    
    @Test
    public void testComparisonOperatorTypeMismatch_GtOnBoolean() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if ((x > 0) > (y > 0)) { " +
            "    x = 0 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("GT on Boolean", code, "Comparison operator requires numeric operands");
    }
    
    @Test
    public void testUndeclaredVariable() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  x = undeclaredVar ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Undeclared Variable", code, "Undeclared variable");
    }
    
    @Test
    public void testVariableUsedAsProcedure() {
        String code = 
            "glob { setX } " +
            "proc { " +
            "  setX (val) { " +
            "    local { } " +
            "    print val " +
            "  } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  setX (42) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Variable Used as Procedure", code, "is a variable");
    }
    
    @Test
    public void testDuplicateGlobalVariable() {
        String code = 
            "glob { x x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Duplicate Global Variable", code, "Duplicate symbol");
    }
    
    @Test
    public void testDuplicateLocalVariable() {
        String code = 
            "glob { } " +
            "proc { " +
            "  test () { " +
            "    local { x x } " +
            "    x = 5 " +
            "  } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Duplicate Local Variable", code, "Duplicate symbol");
    }

    @Test
    public void testFunctionWithNoLocalVariables() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { " +
            "  identity (x) { " +
            "    local { } " +
            "    return x " +
            "  } " +
            "} " +
            "main { " +
            "  var { result } " +
            "  result = identity (42) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Function with No INSTR", code, "Expected instruction");
    }

    @Test
    public void testMultipleFunctionsAndProcedures() {
        String code = 
            "glob { } " +
            "proc { " +
            "  proc1 (x) { local { } print x } " +
            "  proc2 (x y) { local { } print (x plus y) } " +
            "} " +
            "func { " +
            "  func1 (x) { local { } return (x plus 1) } " +
            "  func2 (x y) { local { } return (x mult y) } " +
            "} " +
            "main { " +
            "  var { } " +
            "  proc1 (5) ; " +
            "  proc2 (3 4) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckFails("Multiple Functions and Procedures", code, "Expected ATOM");
    }
    

    // Test Category 3: Edge Cases
    
    @Test
    public void testEmptyProgram() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Empty Program", code);
    }
    
    @Test
    public void testOnlyGlobalVariables() {
        String code = 
            "glob { x y z } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  x = 1 ; " +
            "  y = 2 ; " +
            "  z = 3 ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Only Global Variables", code);
    }
    
    @Test
    public void testOnlyLocalVariables() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { x y z } " +
            "  x = 1 ; " +
            "  y = 2 ; " +
            "  z = 3 ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Only Local Variables", code);
    }
    
    @Test
    public void testProcedureWithNoParameters() {
        String code = 
            "glob { } " +
            "proc { " +
            "  doSomething () { " +
            "    local { } " +
            "    print \"done\" " +
            "  } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  doSomething () ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Procedure with No Parameters", code);
    }
    
    @Test
    public void testDeeplyNestedExpressions() {
        String code = 
            "glob { a b c d } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  a = ((((b plus c) mult d) minus a) div 2) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Deeply Nested Expressions", code);
    }
    
    @Test
    public void testComplexBooleanExpression() {
        String code = 
            "glob { a b c d } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if ((((a > b) and (c > d)) or (a eq c)) and (not (b eq d))) { " +
            "    a = 1 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Complex Boolean Expression", code);
    }
    
    @Test
    public void testSameVariableNameInDifferentScopes() {
        String code = 
            "glob { x } " +
            "proc { " +
            "  proc1 () { " +
            "    local { x } " +
            "    x = 1 " +
            "  } " +
            "} " +
            "func { } " +
            "main { " +
            "  var { x } " +
            "  x = 2 ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Same Variable Name in Different Scopes", code);
    }
    

    // Test Category 4: Specific EXOR Rule Tests
    
    @Test
    public void testUnopExorRule_NumericNeg() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { result } " +
            "  result = (neg x) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("UNOP EXOR: neg numeric", code);
    }
    
    @Test
    public void testUnopExorRule_BooleanNot() {
        String code = 
            "glob { x } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if (not (x > 0)) { " +
            "    x = 1 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("UNOP EXOR: not boolean", code);
    }
    
    @Test
    public void testBinopExorRule_NumericPlus() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { result } " +
            "  result = (x plus y) ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("BINOP EXOR: numeric plus numeric", code);
    }
    
    @Test
    public void testBinopExorRule_BooleanAnd() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if ((x > 0) and (y > 0)) { " +
            "    x = 1 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("BINOP EXOR: boolean and boolean", code);
    }
    
    @Test
    public void testBinopExorRule_ComparisonGt() {
        String code = 
            "glob { x y } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  if (x > y) { " +
            "    x = 1 " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("BINOP EXOR: numeric > numeric = boolean", code);
    }
    

    // Test Category 5: Integration Tests
    
    @Test
    public void testCalculatorProgram() {
        String code = 
            "glob { a b result } " +
            "proc { } " +
            "func { " +
            "  add (x y) { " +
            "    local { sum } " +
            "    sum = (x plus y) " +
            "    ; return sum " +
            "  } " +
            "} " +
            "main { " +
            "  var { } " +
            "  a = 5 ; " +
            "  b = 3 ; " +
            "  result = add (a b) ; " +
            "  print result ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Calculator Program", code);
    }
    
    @Test
    public void testCounterProgram() {
        String code = 
            "glob { counter } " +
            "proc { } " +
            "func { } " +
            "main { " +
            "  var { } " +
            "  counter = 1 ; " +
            "  while (counter > 10) { " +
            "    if ((counter div 2) eq 0) { " +
            "      print \"even\" " +
            "    } else { " +
            "      print \"odd\" " +
            "    } ; " +
            "    counter = (counter plus 1) " +
            "  } ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Counter Program", code);
    }
    
    @Test
    public void testFactorialProgram() {
        String code = 
            "glob { } " +
            "proc { } " +
            "func { " +
            "  factorial (n) { " +
            "    local { result i } " +
            "    result = 1 ; " +
            "    i = 1 ; " +
            "    while (i > n) { " +
            "      result = (result mult i) ; " +
            "      i = (i plus 1) " +
            "    } ; " +
            "    return result " +
            "  } " +
            "} " +
            "main { " +
            "  var { answer } " +
            "  answer = factorial (5) ; " +
            "  print answer ; " +
            "  halt " +
            "}";
        
        assertTypeCheckPasses("Factorial Program", code);
    }
}