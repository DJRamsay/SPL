package semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lexer.Token;
import lexer.TokenType;

public class TypeAnalyzer {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors;
    private final Map<String, TypeInfo> symbolTable;
    
    private String currentContext = null;
    
    public TypeAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.symbolTable = new HashMap<>();
    }
    
    public TypeAnalysisResult analyze() {
        try {
            analyzeSPL_PROG();
        } catch (TypeException e) {
            errors.add(e.getMessage());
        }
        
        return new TypeAnalysisResult(errors.isEmpty(), errors);
    }
    
    // =========================================================================
    // Type System
    // =========================================================================
    
    public enum DataType {
        NUMERIC("numeric"),
        BOOLEAN("boolean"),
        TYPELESS("type-less");  // For procedure/function names
        
        private final String name;
        
        DataType(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private static class TypeInfo {
        final String name;
        final DataType dataType;
        final String context; // procedure/function name for local variables
        
        TypeInfo(String name, DataType dataType, String context) {
            this.name = name;
            this.dataType = dataType;
            this.context = context;
        }
        
        @Override
        public String toString() {
            return name + " (" + dataType + 
                   (context != null ? " in " + context : "") + ")";
        }
    }
    
    public static class TypeAnalysisResult {
        private final boolean success;
        private final List<String> errors;
        
        public TypeAnalysisResult(boolean success, List<String> errors) {
            this.success = success;
            this.errors = errors;
        }
        
        public boolean isSuccess() { return success; }
        public List<String> getErrors() { return errors; }
    }
    
    // =========================================================================
    // Token Management
    // =========================================================================
    
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    private Token expect(TokenType type, String message) throws TypeException {
        if (check(type)) {
            return advance();
        }
        throw new TypeException(message + " at " + where());
    }
    
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }
    
    private Token peek() {
        return tokens.get(current);
    }
    
    private Token previous() {
        return tokens.get(current - 1);
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }
    
    private String where() {
        Token t = isAtEnd() ? previous() : peek();
        return "line " + t.getLine() + ", column " + t.getColumn();
    }
    
    // =========================================================================
    // Type Analysis Implementation
    // =========================================================================
    
    // SPL_PROG ::= glob { VARIABLES } proc { PROCDEFS } func { FUNCDEFS } main { MAINPROG }
    // Semantic: correctly typed if all components are correctly typed
    private void analyzeSPL_PROG() throws TypeException {
        // 1. Global variables
        expect(TokenType.GLOB, "Expected 'glob'");
        expect(TokenType.LBRACE, "Expected '{' after glob");
        analyzeVARIABLES();
        expect(TokenType.RBRACE, "Expected '}' after global variables");
        
        // 2. Procedures
        expect(TokenType.PROC, "Expected 'proc'");
        expect(TokenType.LBRACE, "Expected '{' after proc");
        analyzePROCDEFS();
        expect(TokenType.RBRACE, "Expected '}' after procedures");
        
        // 3. Functions
        expect(TokenType.FUNC, "Expected 'func'");
        expect(TokenType.LBRACE, "Expected '{' after func");
        analyzeFUNCDEFS();
        expect(TokenType.RBRACE, "Expected '}' after functions");
        
        // 4. Main program
        expect(TokenType.MAIN, "Expected 'main'");
        expect(TokenType.LBRACE, "Expected '{' after main");
        analyzeMAINPROG();
        expect(TokenType.RBRACE, "Expected '}' after main program");
    }
    
    // VARIABLES ::= VAR VARIABLES | epsilon
    // Semantic: correctly typed if VAR is numeric and VARIABLES is correctly typed
    private void analyzeVARIABLES() throws TypeException {
        while (check(TokenType.IDENTIFIER)) {
            Token varToken = analyzeVAR();
            addToSymbolTable(varToken.getLexeme(), DataType.NUMERIC, currentContext);
        }
    }
    
    // VAR ::= user-defined-name
    // Semantic: VAR is of type "numeric" (fact)
    private Token analyzeVAR() throws TypeException {
        Token varToken = expect(TokenType.IDENTIFIER, "Expected variable name");
        return varToken;
    }
    
    // PROCDEFS ::= PDEF PROCDEFS | epsilon
    // Semantic: correctly typed if both components are correctly typed
    private void analyzePROCDEFS() throws TypeException {
        while (check(TokenType.IDENTIFIER)) {
            analyzePDEF();
        }
    }
    
    // PDEF ::= NAME ( PARAM ) { BODY }
    // Semantic: correctly typed if NAME is type-less, PARAM is correctly typed, and BODY is correctly typed
    private void analyzePDEF() throws TypeException {
        Token nameToken = parseNAME();
        String procName = nameToken.getLexeme();
        
        checkNameIsTypeless(procName);
        
        addToSymbolTable(procName, DataType.TYPELESS, null);
        
        currentContext = procName;
        
        expect(TokenType.LPAREN, "Expected '(' after procedure name");
        analyzePARAM();
        expect(TokenType.RPAREN, "Expected ')' after parameters");
        
        expect(TokenType.LBRACE, "Expected '{' to start procedure body");
        analyzeBODY();
        expect(TokenType.RBRACE, "Expected '}' to end procedure body");
        
        // Exit procedure
        currentContext = null;
    }
    
    // FUNCDEFS ::= FDEF FUNCDEFS | epsilon
    // Semantic: correctly typed if both components are correctly typed
    private void analyzeFUNCDEFS() throws TypeException {
        while (check(TokenType.IDENTIFIER)) {
            analyzeFDEF();
        }
    }
    
    // FDEF ::= NAME ( PARAM ) { BODY ; return ATOM }
    // Semantic: correctly typed if NAME is type-less, PARAM/BODY are correctly typed, and ATOM is numeric
    private void analyzeFDEF() throws TypeException {
        Token nameToken = parseNAME();
        String funcName = nameToken.getLexeme();
        
        // Check NAME is type-less (not a variable)
        checkNameIsTypeless(funcName);
        
        addToSymbolTable(funcName, DataType.TYPELESS, null);
        
        currentContext = funcName;
        
        expect(TokenType.LPAREN, "Expected '(' after function name");
        analyzePARAM();
        expect(TokenType.RPAREN, "Expected ')' after parameters");
        
        expect(TokenType.LBRACE, "Expected '{' to start function body");
        analyzeBODY();
        expect(TokenType.SEMICOLON, "Expected ';' before return");
        expect(TokenType.RETURN, "Expected 'return' in function");
        
        DataType atomType = analyzeATOM();
        if (atomType != DataType.NUMERIC) {
            throw new TypeException("Return value must be numeric, but got " + atomType + " at " + where());
        }
        
        expect(TokenType.RBRACE, "Expected '}' to end function body");
        
        currentContext = null;
    }
    
    // BODY ::= local { MAXTHREE } ALGO
    // Semantic: correctly typed if MAXTHREE and ALGO are correctly typed
    private void analyzeBODY() throws TypeException {
        expect(TokenType.LOCAL, "Expected 'local'");
        expect(TokenType.LBRACE, "Expected '{' after local");
        analyzeMAXTHREE();
        expect(TokenType.RBRACE, "Expected '}' after local variables");
        analyzeALGO();
    }
    
    // PARAM ::= MAXTHREE
    // Semantic: correctly typed if MAXTHREE is correctly typed
    private void analyzePARAM() throws TypeException {
        analyzeMAXTHREE();
    }
    
    // MAXTHREE ::= VAR [VAR [VAR]] | epsilon
    // Semantic: correctly typed if all VARs are numeric
    private void analyzeMAXTHREE() throws TypeException {
        int count = 0;
        while (check(TokenType.IDENTIFIER) && count < 3) {
            Token varToken = analyzeVAR();
            addToSymbolTable(varToken.getLexeme(), DataType.NUMERIC, currentContext);
            count++;
        }
    }
    
    // MAINPROG ::= var { VARIABLES } ALGO
    // Semantic: correctly typed if VARIABLES and ALGO are correctly typed
    private void analyzeMAINPROG() throws TypeException {
        expect(TokenType.VAR, "Expected 'var'");
        expect(TokenType.LBRACE, "Expected '{' after var");
        currentContext = "main";
        analyzeVARIABLES();
        expect(TokenType.RBRACE, "Expected '}' after main variables");
        analyzeALGO();
        currentContext = null;
    }
    
    // ALGO ::= INSTR [; ALGO]
    // Semantic: correctly typed if all INSTR are correctly typed
    private void analyzeALGO() throws TypeException {
        analyzeINSTR();
        
        while (match(TokenType.SEMICOLON)) {
            if (check(TokenType.RBRACE) || check(TokenType.RETURN)) {
                break;
            }
            analyzeINSTR();
        }
    }
    
    // INSTR ::= halt | print OUTPUT | NAME ( INPUT ) | ASSIGN | LOOP | BRANCH
    // Semantic: correctly typed based on which alternative
    private void analyzeINSTR() throws TypeException {
        if (match(TokenType.HALT)) {
            return;
        }
        
        // print OUTPUT
        if (match(TokenType.PRINT)) {
            analyzeOUTPUT();
            return;
        }
        
        if (check(TokenType.IDENTIFIER) && lookahead(1).getType() == TokenType.LPAREN) {
            Token nameToken = parseNAME();
            checkNameIsTypeless(nameToken.getLexeme());
            
            expect(TokenType.LPAREN, "Expected '(' after procedure name");
            analyzeINPUT();
            expect(TokenType.RPAREN, "Expected ')' after arguments");
            return;
        }
        
        if (check(TokenType.IDENTIFIER) && lookahead(1).getType() == TokenType.ASSIGN) {
            analyzeASSIGN();
            return;
        }
        
        // LOOP
        if (check(TokenType.WHILE) || check(TokenType.DO)) {
            analyzeLOOP();
            return;
        }
        
        // BRANCH
        if (check(TokenType.IF)) {
            analyzeBRANCH();
            return;
        }
        
        throw new TypeException("Expected instruction at " + where());
    }
    
    // ASSIGN ::= VAR = NAME ( INPUT ) | VAR = TERM
    // Semantic: VAR must be numeric, and either NAME is type-less with correct INPUT, or TERM is numeric
    private void analyzeASSIGN() throws TypeException {
        Token varToken = parseVAR();
        String varName = varToken.getLexeme();
        
        checkVariableType(varName, DataType.NUMERIC);
        
        expect(TokenType.ASSIGN, "Expected '='");
        
        // Check if it's a function call: NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookahead(1).getType() == TokenType.LPAREN) {
            Token nameToken = parseNAME();
            checkNameIsTypeless(nameToken.getLexeme());
            
            expect(TokenType.LPAREN, "Expected '(' after function name");
            analyzeINPUT();
            expect(TokenType.RPAREN, "Expected ')' after arguments");
        } else {
            // VAR = TERM
            DataType termType = analyzeTERM();
            if (termType != DataType.NUMERIC) {
                throw new TypeException("Assignment requires numeric type, but got " + termType + " at " + where());
            }
        }
    }
    
    // LOOP ::= while TERM { ALGO } | do { ALGO } until TERM
    // Semantic: TERM must be boolean, ALGO must be correctly typed
    private void analyzeLOOP() throws TypeException {
        if (match(TokenType.WHILE)) {
            // while TERM { ALGO }
            DataType termType = analyzeTERM();
            if (termType != DataType.BOOLEAN) {
                throw new TypeException("While condition must be boolean, but got " + termType + " at " + where());
            }
            
            expect(TokenType.LBRACE, "Expected '{' after while condition");
            analyzeALGO();
            expect(TokenType.RBRACE, "Expected '}' after while body");
            
        } else if (match(TokenType.DO)) {
            // do { ALGO } until TERM
            expect(TokenType.LBRACE, "Expected '{' after do");
            analyzeALGO();
            expect(TokenType.RBRACE, "Expected '}' after do body");
            
            expect(TokenType.UNTIL, "Expected 'until' after do body");
            
            DataType termType = analyzeTERM();
            if (termType != DataType.BOOLEAN) {
                throw new TypeException("Until condition must be boolean, but got " + termType + " at " + where());
            }
        } else {
            throw new TypeException("Expected 'while' or 'do' for loop at " + where());
        }
    }
    
    // BRANCH ::= if TERM { ALGO } [else { ALGO }]
    // Semantic: TERM must be boolean, both ALGOs must be correctly typed
    private void analyzeBRANCH() throws TypeException {
        expect(TokenType.IF, "Expected 'if'");
        
        DataType termType = analyzeTERM();
        if (termType != DataType.BOOLEAN) {
            throw new TypeException("If condition must be boolean, but got " + termType + " at " + where());
        }
        
        expect(TokenType.LBRACE, "Expected '{' after if condition");
        analyzeALGO();
        expect(TokenType.RBRACE, "Expected '}' after if body");
        
        if (match(TokenType.ELSE)) {
            expect(TokenType.LBRACE, "Expected '{' after else");
            analyzeALGO();
            expect(TokenType.RBRACE, "Expected '}' after else body");
        }
    }
    
    // OUTPUT ::= ATOM | string
    // Semantic: correctly typed if ATOM is numeric, or it's a string (always correct)
    private void analyzeOUTPUT() throws TypeException {
        if (match(TokenType.STRING)) {
            return;
        }
        
        DataType atomType = analyzeATOM();
        if (atomType != DataType.NUMERIC) {
            throw new TypeException("Print output must be numeric or string, but got " + atomType + " at " + where());
        }
    }
    
    // INPUT ::= [ATOM [ATOM [ATOM]]] | epsilon
    // Semantic: correctly typed if all ATOMs are numeric
    private void analyzeINPUT() throws TypeException {
        int count = 0;
        while ((check(TokenType.IDENTIFIER) || check(TokenType.NUMBER)) && count < 3) {
            DataType atomType = analyzeATOM();
            if (atomType != DataType.NUMERIC) {
                throw new TypeException("Function/procedure argument must be numeric, but got " + atomType + " at " + where());
            }
            count++;
        }
    }
    
    // ATOM ::= VAR | number
    // Semantic: ATOM is numeric if VAR is numeric, or if it's a number
    private DataType analyzeATOM() throws TypeException {
        if (match(TokenType.NUMBER)) {
            return DataType.NUMERIC;
        }
        
        if (check(TokenType.IDENTIFIER)) {
            Token varToken = parseVAR();
            String varName = varToken.getLexeme();
            
            // Check that variable is numeric
            DataType varType = getVariableType(varName);
            if (varType != DataType.NUMERIC) {
                throw new TypeException("Expected numeric variable, but '" + varName + "' is " + varType + " at " + where());
            }
            return DataType.NUMERIC;
        }
        
        throw new TypeException("Expected ATOM (variable or number) at " + where());
    }
    
    // TERM ::= ATOM | ( UNOP TERM ) | ( TERM BINOP TERM )
    // Semantic: Type depends on operators and operands (see PDF for EXOR rules)
    private DataType analyzeTERM() throws TypeException {
        // TERM ::= ATOM
        if (check(TokenType.IDENTIFIER) || check(TokenType.NUMBER)) {
            return analyzeATOM(); // Returns NUMERIC
        }
        
        // TERM ::= ( UNOP TERM ) or ( TERM BINOP TERM )
        if (match(TokenType.LPAREN)) {
            // Check if it's UNOP
            if (check(TokenType.NEG) || check(TokenType.NOT)) {
                Token unopToken = parseUNOP();
                DataType unopType = getUnopType(unopToken);
                DataType termType = analyzeTERM();
                
                expect(TokenType.RPAREN, "Expected ')' to close term");
                
                // UNOP and TERM must have same type
                if (unopType == DataType.NUMERIC && termType == DataType.NUMERIC) {
                    return DataType.NUMERIC;
                } else if (unopType == DataType.BOOLEAN && termType == DataType.BOOLEAN) {
                    return DataType.BOOLEAN;
                } else {
                    throw new TypeException("Type mismatch: " + unopType + " operator with " + termType + " operand at " + where());
                }
            } else {
                // It's ( TERM BINOP TERM )
                DataType leftType = analyzeTERM();
                Token binopToken = parseBINOP();
                DataType binopType = getBinopType(binopToken);
                DataType rightType = analyzeTERM();
                
                expect(TokenType.RPAREN, "Expected ')' to close term");
                
                // Type checking based on BINOP type
                if (binopType == DataType.NUMERIC) {
                    if (leftType != DataType.NUMERIC || rightType != DataType.NUMERIC) {
                        throw new TypeException("Numeric operator requires numeric operands at " + where());
                    }
                    return DataType.NUMERIC;
                    
                } else if (binopType == DataType.BOOLEAN) {
                    if (leftType != DataType.BOOLEAN || rightType != DataType.BOOLEAN) {
                        throw new TypeException("Boolean operator requires boolean operands at " + where());
                    }
                    return DataType.BOOLEAN;
                    
                } else { // binopType is comparison
                    // Both operands must be numeric, result is boolean
                    if (leftType != DataType.NUMERIC || rightType != DataType.NUMERIC) {
                        throw new TypeException("Comparison operator requires numeric operands at " + where());
                    }
                    return DataType.BOOLEAN;
                }
            }
        }
        
        throw new TypeException("Invalid term at " + where());
    }
    
    // Helper methods for parsing
    private Token parseNAME() throws TypeException {
        return expect(TokenType.IDENTIFIER, "Expected name");
    }
    
    private Token parseVAR() throws TypeException {
        return expect(TokenType.IDENTIFIER, "Expected variable name");
    }
    
    private Token parseUNOP() throws TypeException {
        if (match(TokenType.NEG, TokenType.NOT)) {
            return previous();
        }
        throw new TypeException("Expected unary operator 'neg' or 'not'");
    }
    
    private Token parseBINOP() throws TypeException {
        if (match(TokenType.EQ, TokenType.GT, TokenType.OR, TokenType.AND, 
                  TokenType.PLUS, TokenType.MINUS, TokenType.MULT, TokenType.DIV)) {
            return previous();
        }
        throw new TypeException("Expected binary operator");
    }
    
    private Token lookahead(int distance) {
        int index = current + distance;
        if (index >= tokens.size()) {
            return new Token(TokenType.EOF, "", 0, 0);
        }
        return tokens.get(index);
    }
    
    // =========================================================================
    // Type Checking Helpers
    // =========================================================================
    
    // Get the type of a unary operator
    private DataType getUnopType(Token unop) {
        switch (unop.getType()) {
            case NEG:
                return DataType.NUMERIC; 
            case NOT:
                return DataType.BOOLEAN;
            default:
                return null;
        }
    }
    
    // Get the type of a binary operator
    // Returns NUMERIC for arithmetic, BOOLEAN for logical, or a special marker for comparison
    private DataType getBinopType(Token binop) {
        switch (binop.getType()) {
            case PLUS:
            case MINUS:
            case MULT:
            case DIV:
                return DataType.NUMERIC;
                
            case OR:
            case AND:
                return DataType.BOOLEAN;
                
            case GT:
            case EQ:
                // Comparison operators: take numeric operands, return boolean
                return DataType.TYPELESS;
                
            default:
                return null;
        }
    }
    
    private void checkNameIsTypeless(String name) throws TypeException {
        if (symbolTable.containsKey(name)) {
            TypeInfo info = symbolTable.get(name);
            if (info.dataType != DataType.TYPELESS) {
                throw new TypeException("'" + name + "' is a variable (" + info.dataType + "), not a procedure/function at " + where());
            }
        }
    }
    
    // Check that a variable has a specific type
    private void checkVariableType(String varName, DataType expectedType) throws TypeException {
        DataType actualType = getVariableType(varName);
        if (actualType != expectedType) {
            throw new TypeException("Variable '" + varName + "' has type " + actualType + 
                                  ", but expected " + expectedType + " at " + where());
        }
    }
    
    // Get the type of a variable (searches through contexts)
    private DataType getVariableType(String varName) throws TypeException {
        if (currentContext != null) {
            String key = varName + "@" + currentContext;
            if (symbolTable.containsKey(key)) {
                return symbolTable.get(key).dataType;
            }
        }
        
        // Then check global scope
        if (symbolTable.containsKey(varName)) {
            return symbolTable.get(varName).dataType;
        }
        
        throw new TypeException("Undeclared variable: '" + varName + "' at " + where());
    }
    
    // Add a symbol to the symbol table
    private void addToSymbolTable(String name, DataType dataType, String context) throws TypeException {
        String key = context != null ? name + "@" + context : name;
        
        // Check for conflicts
        if (symbolTable.containsKey(key)) {
            TypeInfo existing = symbolTable.get(key);
            throw new TypeException("Duplicate symbol '" + name + "' " + 
                                  (context != null ? "in " + context : "at global scope"));
        }
        
        symbolTable.put(key, new TypeInfo(name, dataType, context));
    }
}

class TypeException extends Exception {
    public TypeException(String message) {
        super("Type Error: " + message);
    }
}