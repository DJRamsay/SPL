package semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lexer.Token;
import lexer.TokenType;

public class ScopeAnalyzer {
    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors;
    private final Map<String, SymbolInfo> symbolTable;
    private final Set<String> currentScopeVariables;
    
    // Scope tracking
    private Scope currentScope = Scope.EVERYWHERE;
    private String currentProcedure = null;
    private String currentFunction = null;
    
    public ScopeAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.errors = new ArrayList<>();
        this.symbolTable = new HashMap<>();
        this.currentScopeVariables = new HashSet<>();
    }
    
    public ScopeAnalysisResult analyze() {
        try {
            analyzeSPL_PROG();
        } catch (ScopeException e) {
            errors.add(e.getMessage());
        }
        
        return new ScopeAnalysisResult(errors.isEmpty(), errors);
    }
    
    // =========================================================================
    // Scope and Symbol Types
    // =========================================================================
    
    private enum Scope {
        EVERYWHERE("Everywhere"),
        GLOBAL("Global"),
        PROCEDURE("Procedure"),
        FUNCTION("Function"),
        MAIN("Main"),
        LOCAL("Local");
        
        private final String name;
        
        Scope(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private enum SymbolType {
        VARIABLE("variable"),
        PROCEDURE("procedure"),
        FUNCTION("function"),
        PARAMETER("parameter");
        
        private final String name;
        
        SymbolType(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private static class SymbolInfo {
        final String name;
        final SymbolType type;
        final Scope scope;
        final String context; // procedure/function name for local scope
        
        SymbolInfo(String name, SymbolType type, Scope scope, String context) {
            this.name = name;
            this.type = type;
            this.scope = scope;
            this.context = context;
        }
        
        @Override
        public String toString() {
            return name + " (" + type + " in " + scope + 
                   (context != null ? " of " + context : "") + ")";
        }
    }
    
    public static class ScopeAnalysisResult {
        private final boolean success;
        private final List<String> errors;
        
        public ScopeAnalysisResult(boolean success, List<String> errors) {
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
    
    private Token expect(TokenType type, String message) throws ScopeException {
        if (check(type)) {
            return advance();
        }
        throw new ScopeException(message + " at " + where());
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
    
    private Token lookahead(int distance) {
        int index = current + distance;
        if (index >= tokens.size()) {
            return new Token(TokenType.EOF, "", 0, 0);
        }
        return tokens.get(index);
    }
    
    // =========================================================================
    // Scope Analysis Rules Implementation
    // =========================================================================
    
    // SPL_PROG ::= glob { VARIABLES } proc { PROCDEFS } func { FUNCDEFS } main { MAINPROG }
    private void analyzeSPL_PROG() throws ScopeException {
        currentScope = Scope.EVERYWHERE;
        
        // 1. Global variables
        expect(TokenType.GLOB, "Expected 'glob'");
        expect(TokenType.LBRACE, "Expected '{' after glob");
        currentScope = Scope.GLOBAL;
        analyzeVARIABLES();
        expect(TokenType.RBRACE, "Expected '}' after global variables");
        currentScope = Scope.EVERYWHERE;
        
        // 2. Procedures
        expect(TokenType.PROC, "Expected 'proc'");
        expect(TokenType.LBRACE, "Expected '{' after proc");
        currentScope = Scope.PROCEDURE;
        analyzePROCDEFS();
        expect(TokenType.RBRACE, "Expected '}' after procedures");
        currentScope = Scope.EVERYWHERE;
        
        // 3. Functions
        expect(TokenType.FUNC, "Expected 'func'");
        expect(TokenType.LBRACE, "Expected '{' after func");
        currentScope = Scope.FUNCTION;
        analyzeFUNCDEFS();
        expect(TokenType.RBRACE, "Expected '}' after functions");
        currentScope = Scope.EVERYWHERE;
        
        // 4. Main program
        expect(TokenType.MAIN, "Expected 'main'");
        expect(TokenType.LBRACE, "Expected '{' after main");
        currentScope = Scope.MAIN;
        analyzeMAINPROG();
        expect(TokenType.RBRACE, "Expected '}' after main program");
        currentScope = Scope.EVERYWHERE;
        
        // Check global naming conflicts (Rule: no variable name identical with function/procedure name)
        checkGlobalNamingConflicts();
    }
    
    // Check global naming conflicts
    private void checkGlobalNamingConflicts() {
        Set<String> variableNames = new HashSet<>();
        Set<String> procedureNames = new HashSet<>();
        Set<String> functionNames = new HashSet<>();
        
        for (SymbolInfo symbol : symbolTable.values()) {
            if (symbol.scope == Scope.GLOBAL || symbol.scope == Scope.EVERYWHERE) {
                switch (symbol.type) {
                    case VARIABLE:
                        variableNames.add(symbol.name);
                        break;
                    case PROCEDURE:
                        procedureNames.add(symbol.name);
                        break;
                    case FUNCTION:
                        functionNames.add(symbol.name);
                        break;
                    case PARAMETER:
                        break;
                }
            }
        }
        
        // Check conflicts
        for (String varName : variableNames) {
            if (procedureNames.contains(varName)) {
                errors.add("Naming error: variable '" + varName + "' conflicts with procedure name");
            }
            if (functionNames.contains(varName)) {
                errors.add("Naming error: variable '" + varName + "' conflicts with function name");
            }
        }
        
        for (String procName : procedureNames) {
            if (functionNames.contains(procName)) {
                errors.add("Naming error: procedure '" + procName + "' conflicts with function name");
            }
        }
    }
    
    // VARIABLES ::= VAR VARIABLES | epsilon
    private void analyzeVARIABLES() throws ScopeException {
        Set<String> declaredNames = new HashSet<>();
        
        while (check(TokenType.VAR) || check(TokenType.IDENTIFIER)) {
            Token name;
            if (check(TokenType.VAR)) {
                advance(); // consume 'var' if present
                name = expect(TokenType.IDENTIFIER, "Expected variable name after 'var'");
            } else {
                name = expect(TokenType.IDENTIFIER, "Expected variable name");
            }
            
            String varName = name.getLexeme();
            
            // Check for double declaration in same scope
            if (declaredNames.contains(varName)) {
                throw new ScopeException("Double declaration of variable '" + varName + "' in " + currentScope + " scope");
            }
            declaredNames.add(varName);
            
            // Add to symbol table
            SymbolInfo symbol = new SymbolInfo(varName, SymbolType.VARIABLE, currentScope, null);
            if (symbolTable.containsKey(varName) && isSameScopeConflict(symbolTable.get(varName), symbol)) {
                throw new ScopeException("Duplicate symbol '" + varName + "' in " + currentScope + " scope");
            }
            symbolTable.put(varName, symbol);
            
            // Optional semicolon
            if (check(TokenType.SEMICOLON)) {
                advance();
            }
        }
    }
    
    // PROCDEFS ::= PDEF PROCDEFS | epsilon
    private void analyzePROCDEFS() throws ScopeException {
        Set<String> declaredProcedures = new HashSet<>();
        
        while (check(TokenType.IDENTIFIER)) {
            Token name = peek();
            String procName = name.getLexeme();
            
            if (declaredProcedures.contains(procName)) {
                throw new ScopeException("Double declaration of procedure '" + procName + "'");
            }
            declaredProcedures.add(procName);
            
            analyzePDEF();
        }
    }
    
    // PDEF ::= NAME { PARAM } { BODY }
    private void analyzePDEF() throws ScopeException {
        Token name = parseNAME();
        String procName = name.getLexeme();
        
        // Add procedure to symbol table
        symbolTable.put(procName, new SymbolInfo(procName, SymbolType.PROCEDURE, Scope.EVERYWHERE, null));
        
        expect(TokenType.LPAREN, "Expected '(' for procedure parameters");
        
        // Enter procedure local scope
        String savedProcedure = currentProcedure;
        currentProcedure = procName;
        currentScope = Scope.LOCAL;
        currentScopeVariables.clear();
        
        analyzePARAM();
        
        expect(TokenType.RPAREN, "Expected ')' after procedure parameters");
        expect(TokenType.LBRACE, "Expected '{' for procedure body");
        
        analyzeBODY();
        
        expect(TokenType.RBRACE, "Expected '}' after procedure body");
        
        // Exit procedure scope
        currentScope = Scope.PROCEDURE;
        currentProcedure = savedProcedure;
    }
    
    // FUNCDEFS ::= FDEF FUNCDEFS | epsilon
    private void analyzeFUNCDEFS() throws ScopeException {
        Set<String> declaredFunctions = new HashSet<>();
        
        while (check(TokenType.IDENTIFIER)) {
            Token name = peek();
            String funcName = name.getLexeme();
            
            if (declaredFunctions.contains(funcName)) {
                throw new ScopeException("Double declaration of function '" + funcName + "'");
            }
            declaredFunctions.add(funcName);
            
            analyzeFDEF();
        }
    }
    
    // FDEF ::= NAME { PARAM } { BODY ; return ATOM }
    private void analyzeFDEF() throws ScopeException {
        Token name = parseNAME();
        String funcName = name.getLexeme();
        
        // Add function to symbol table
        symbolTable.put(funcName, new SymbolInfo(funcName, SymbolType.FUNCTION, Scope.EVERYWHERE, null));
        
        expect(TokenType.LPAREN, "Expected '(' for function parameters");
        
        // Enter function local scope
        String savedFunction = currentFunction;
        currentFunction = funcName;
        currentScope = Scope.LOCAL;
        currentScopeVariables.clear();
        
        analyzePARAM();
        
        expect(TokenType.RPAREN, "Expected ')' after function parameters");
        expect(TokenType.LBRACE, "Expected '{' for function body");
        
        analyzeBODY();
        
        expect(TokenType.SEMICOLON, "Expected ';' before return");
        expect(TokenType.RETURN, "Expected 'return' in function");
        analyzeATOM(); // Analyze the return atom
        
        expect(TokenType.RBRACE, "Expected '}' after function body");
        
        // Exit function scope
        currentScope = Scope.FUNCTION;
        currentFunction = savedFunction;
    }
    
    // BODY ::= local { MAXTHREE } ALGO
    private void analyzeBODY() throws ScopeException {
        expect(TokenType.LOCAL, "Expected 'local'");
        expect(TokenType.LBRACE, "Expected '{' after local");
        
        // Analyze local variables
        Set<String> localVars = analyzeMAXTHREE();
        
        // Check for shadowing of parameter names by local variables
        for (String localVar : localVars) {
            if (currentScopeVariables.contains(localVar)) {
                throw new ScopeException("Shadowing of parameter '" + localVar + "' by local variable declaration");
            }
            
            // Add local variable to symbol table
            String context = currentProcedure != null ? currentProcedure : currentFunction;
            symbolTable.put(localVar, new SymbolInfo(localVar, SymbolType.VARIABLE, Scope.LOCAL, context));
        }
        
        expect(TokenType.RBRACE, "Expected '}' after local variables");
        analyzeALGO();
    }
    
    // PARAM ::= MAXTHREE
    private void analyzePARAM() throws ScopeException {
        Set<String> params = analyzeMAXTHREE();
        currentScopeVariables.addAll(params);
        
        // Add parameters to symbol table
        for (String param : params) {
            String context = currentProcedure != null ? currentProcedure : currentFunction;
            symbolTable.put(param, new SymbolInfo(param, SymbolType.PARAMETER, Scope.LOCAL, context));
        }
    }
    
    // MAXTHREE ::= VAR | VAR VAR | VAR VAR VAR | epsilon
    private Set<String> analyzeMAXTHREE() throws ScopeException {
        Set<String> names = new HashSet<>();
        int count = 0;
        
        while ((check(TokenType.VAR) || check(TokenType.IDENTIFIER)) && count < 3) {
            Token name;
            if (check(TokenType.VAR)) {
                advance(); // consume 'var' if present
                name = expect(TokenType.IDENTIFIER, "Expected variable name after 'var'");
            } else {
                name = expect(TokenType.IDENTIFIER, "Expected variable name");
            }
            
            String varName = name.getLexeme();
            
            // Check for double declaration in parameters
            if (names.contains(varName)) {
                throw new ScopeException("Double declaration of parameter '" + varName + "'");
            }
            names.add(varName);
            count++;
        }
        
        return names;
    }
    
    // MAINPROG ::= var { VARIABLES } ALGO
    private void analyzeMAINPROG() throws ScopeException {
        expect(TokenType.VAR, "Expected 'var' in main program");
        expect(TokenType.LBRACE, "Expected '{' after var");
        analyzeVARIABLES();
        expect(TokenType.RBRACE, "Expected '}' after main variables");
        analyzeALGO();
    }
    
    // ALGO ::= INSTR | INSTR ; ALGO
    private void analyzeALGO() throws ScopeException {
        analyzeINSTR();
        
        while (check(TokenType.SEMICOLON)) {
            advance();
            if (check(TokenType.RBRACE) || check(TokenType.RETURN) || check(TokenType.EOF)) {
                break;
            }
            analyzeINSTR();
        }
    }
    
    // INSTR ::= halt | print OUTPUT | NAME ( INPUT ) | ASSIGN | LOOP | BRANCH
    private void analyzeINSTR() throws ScopeException {
        if (match(TokenType.HALT)) {
            return;
        }
        
        if (match(TokenType.PRINT)) {
            analyzeOUTPUT();
            return;
        }
        
        if (check(TokenType.IDENTIFIER)) {
            // Could be procedure call or assignment
            if (lookahead(1).getType() == TokenType.LPAREN) {
                // Procedure call: NAME ( INPUT )
                Token name = parseNAME();
                checkNameExists(name.getLexeme(), SymbolType.PROCEDURE);
                expect(TokenType.LPAREN, "Expected '(' for procedure call");
                analyzeINPUT();
                expect(TokenType.RPAREN, "Expected ')' after procedure call");
                return;
            } else if (lookahead(1).getType() == TokenType.ASSIGN) {
                analyzeASSIGN();
                return;
            }
        }
        
        if (check(TokenType.WHILE) || check(TokenType.DO)) {
            analyzeLOOP();
            return;
        }
        
        if (check(TokenType.IF)) {
            analyzeBRANCH();
            return;
        }
        
        throw new ScopeException("Invalid instruction at " + where());
    }
    
    // ASSIGN ::= VAR = NAME ( INPUT ) | VAR = TERM
    private void analyzeASSIGN() throws ScopeException {
        Token varToken = parseVAR();
        String varName = varToken.getLexeme();
        
        // Check if variable is declared
        checkVariableDeclaration(varName);
        
        expect(TokenType.ASSIGN, "Expected '=' in assignment");
        
        if (check(TokenType.IDENTIFIER) && lookahead(1).getType() == TokenType.LPAREN) {
            // Function call assignment: VAR = NAME ( INPUT )
            Token funcName = parseNAME();
            checkNameExists(funcName.getLexeme(), SymbolType.FUNCTION);
            expect(TokenType.LPAREN, "Expected '(' for function call");
            analyzeINPUT();
            expect(TokenType.RPAREN, "Expected ')' after function call");
        } else {
            // Simple assignment: VAR = TERM
            analyzeTERM();
        }
    }
    
    // LOOP ::= while TERM { ALGO } | do { ALGO } until TERM
   private void analyzeLOOP() throws ScopeException {
    if (match(TokenType.WHILE)) {
        analyzeTERM();
        expect(TokenType.LBRACE, "Expected '{' for while body");
        analyzeALGO();
        expect(TokenType.RBRACE, "Expected '}' after while body");
    } else if (match(TokenType.DO)) {
        expect(TokenType.LBRACE, "Expected '{' after do");
        analyzeALGO();
        expect(TokenType.RBRACE, "Expected '}' after do body");
        expect(TokenType.UNTIL, "Expected 'until' after do body");
        analyzeTERM();
    } else {
        throw new ScopeException("Expected 'while' or 'do' for loop");
    }
}
    
    // BRANCH ::= if TERM { ALGO } [ else { ALGO } ]
    private void analyzeBRANCH() throws ScopeException {
        expect(TokenType.IF, "Expected 'if'");
        analyzeTERM();
        expect(TokenType.LBRACE, "Expected '{' for if body");
        analyzeALGO();
        expect(TokenType.RBRACE, "Expected '}' after if body");
        
        if (match(TokenType.ELSE)) {
            expect(TokenType.LBRACE, "Expected '{' for else body");
            analyzeALGO();
            expect(TokenType.RBRACE, "Expected '}' after else body");
        }
    }
    
    // OUTPUT ::= ATOM | string
    private void analyzeOUTPUT() throws ScopeException {
        if (match(TokenType.STRING)) {
            return;
        }
        analyzeATOM();
    }
    
    // INPUT ::= ATOM [ATOM [ATOM]] | epsilon
    private void analyzeINPUT() throws ScopeException {
        int count = 0;
        while ((check(TokenType.IDENTIFIER) || check(TokenType.NUMBER)) && count < 3) {
            analyzeATOM();
            count++;
        }
    }
    
    // TERM ::= ATOM | ( UNOP TERM ) | ( TERM BINOP TERM )
    private void analyzeTERM() throws ScopeException {
        if (check(TokenType.IDENTIFIER) || check(TokenType.NUMBER)) {
            analyzeATOM();
            return;
        }
        
        if (match(TokenType.LPAREN)) {
            if (check(TokenType.NEG) || check(TokenType.NOT)) {
                parseUNOP();
                analyzeTERM();
            } else {
                analyzeTERM();
                parseBINOP();
                analyzeTERM();
            }
            expect(TokenType.RPAREN, "Expected ')' to close term");
            return;
        }
        
        throw new ScopeException("Invalid term at " + where());
    }
    
    // ATOM ::= VAR | number
    private void analyzeATOM() throws ScopeException {
        if (match(TokenType.NUMBER)) {
            return;
        }
        
        if (check(TokenType.IDENTIFIER)) {
            Token varToken = parseVAR();
            checkVariableDeclaration(varToken.getLexeme());
            return;
        }
        
        throw new ScopeException("Expected ATOM (variable or number) at " + where());
    }
    
    // Helper parsing methods
    private Token parseNAME() throws ScopeException {
        return expect(TokenType.IDENTIFIER, "Expected name");
    }
    
    private Token parseVAR() throws ScopeException {
        return expect(TokenType.IDENTIFIER, "Expected variable name");
    }
    
    private Token parseUNOP() throws ScopeException {
        if (match(TokenType.NEG, TokenType.NOT)) {
            return previous();
        }
        throw new ScopeException("Expected unary operator 'neg' or 'not'");
    }
    
    private Token parseBINOP() throws ScopeException {
        if (match(TokenType.EQ, TokenType.GT, TokenType.OR, TokenType.AND, 
                  TokenType.PLUS, TokenType.MINUS, TokenType.MULT, TokenType.DIV)) {
            return previous();
        }
        throw new ScopeException("Expected binary operator");
    }

    
    // =========================================================================
    // Semantic Checking Helpers
    // =========================================================================
    
    private void checkVariableDeclaration(String varName) throws ScopeException {
        if (!symbolTable.containsKey(varName)) {
            // Check if it's a parameter or local variable in current procedure/function
            boolean found = false;
            
            if (currentProcedure != null || currentFunction != null) {
                // Check parameters and local variables in current context
                for (SymbolInfo symbol : symbolTable.values()) {
                    if (symbol.name.equals(varName) && 
                        (symbol.type == SymbolType.PARAMETER || 
                         (symbol.type == SymbolType.VARIABLE && symbol.scope == Scope.LOCAL))) {
                        String context = currentProcedure != null ? currentProcedure : currentFunction;
                        if (symbol.context != null && symbol.context.equals(context)) {
                            found = true;
                            break;
                        }
                    }
                }
            }
            
            // Check global variables
            if (!found) {
                for (SymbolInfo symbol : symbolTable.values()) {
                    if (symbol.name.equals(varName) && symbol.type == SymbolType.VARIABLE && 
                        (symbol.scope == Scope.GLOBAL || symbol.scope == Scope.MAIN)) {
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                throw new ScopeException("Undeclared variable: " + varName);
            }
        }
    }
    
    private void checkNameExists(String name, SymbolType expectedType) throws ScopeException {
        if (!symbolTable.containsKey(name)) {
            throw new ScopeException("Undeclared " + expectedType + ": " + name);
        }
        
        SymbolInfo symbol = symbolTable.get(name);
        if (symbol.type != expectedType) {
            throw new ScopeException("'" + name + "' is a " + symbol.type + ", not a " + expectedType);
        }
    }
    
    private boolean isSameScopeConflict(SymbolInfo existing, SymbolInfo newSymbol) {
        // Variables can have same name in different scopes
        if (existing.type == SymbolType.VARIABLE && newSymbol.type == SymbolType.VARIABLE) {
            return existing.scope == newSymbol.scope && 
                   ((existing.context == null && newSymbol.context == null) || 
                    (existing.context != null && existing.context.equals(newSymbol.context)));
        }
        
        // Procedures/functions must be unique globally
        return (existing.type == SymbolType.PROCEDURE || existing.type == SymbolType.FUNCTION) &&
               (newSymbol.type == SymbolType.PROCEDURE || newSymbol.type == SymbolType.FUNCTION);
    }
}

class ScopeException extends Exception {
    public ScopeException(String message) {
        super("Scope Error: " + message);
    }
}