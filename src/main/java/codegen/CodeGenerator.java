package codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import lexer.Token;
import lexer.TokenType;

/**
 * SPL -> Target code generator that works directly with Token types.
 * Properly uses TokenType enum instead of string comparisons.
 */
public class CodeGenerator {

    private final List<Token> tokens;
    private int current = 0;
    private final StringBuilder out = new StringBuilder();
    private final LabelGenerator labels = new LabelGenerator();

    public CodeGenerator(List<Token> tokens) {
        this.tokens = tokens;
    }

    public String generate() {
        // program ::= [glob block] [proc block] [func block] main block
        while (!isAtEnd()) {
            if (match(TokenType.GLOB)) {
                skipBraceBlock(); // ignore globals
                continue;
            }
            if (match(TokenType.PROC)) {
                generateProcDefs();
                continue;
            }
            if (match(TokenType.FUNC)) {
                generateFuncDefs();
                continue;
            }
            if (match(TokenType.MAIN)) {
                generateMain();
                break;
            }
            // Anything else: advance to avoid infinite loop
            advance();
        }
        return out.toString();
    }

    /**
     * Generate code for procedure definitions.
     * PROCDEFS ::= PDEF PROCDEFS | ε
     * PDEF ::= NAME ( PARAM ) { BODY }
     */
    private void generateProcDefs() {
        expect(TokenType.LBRACE, "Expected '{' to start proc block");
        
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            // Each procedure definition
            if (check(TokenType.IDENTIFIER)) {
                String procName = advance().getLexeme();
                
                // Parse parameters
                List<String> params = parseParamList();
                
                // Generate procedure header
                emit("REM PROC " + procName + joinParams(params));
                
                // Process BODY: local { MAXTHREE } ALGO
                expect(TokenType.LBRACE, "Expected '{' to start procedure body");
                
                // Optional local variables
                if (match(TokenType.LOCAL)) {
                    expect(TokenType.LBRACE, "Expected '{' after local");
                    skipVariables();
                    expect(TokenType.RBRACE, "Expected '}' after local variables");
                }
                
                // Generate ALGO
                generateAlgoBlock();
                
                expect(TokenType.RBRACE, "Expected '}' to end procedure body");
                emit("RETURN");
            } else {
                advance();
            }
        }
        
        expect(TokenType.RBRACE, "Expected '}' to end proc block");
    }

    /**
     * Generate code for function definitions.
     * FUNCDEFS ::= FDEF FUNCDEFS | ε
     * FDEF ::= NAME ( PARAM ) { BODY ; return ATOM }
     */
    private void generateFuncDefs() {
        expect(TokenType.LBRACE, "Expected '{' to start func block");
        
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            // Each function definition
            if (check(TokenType.IDENTIFIER)) {
                String funcName = advance().getLexeme();
                
                // Parse parameters
                List<String> params = parseParamList();
                
                // Generate function header
                emit("REM FUNC " + funcName + joinParams(params));
                
                // Process BODY: local { MAXTHREE } ALGO ; return ATOM
                expect(TokenType.LBRACE, "Expected '{' to start function body");
                
                // Optional local variables
                if (match(TokenType.LOCAL)) {
                    expect(TokenType.LBRACE, "Expected '{' after local");
                    skipVariables();
                    expect(TokenType.RBRACE, "Expected '}' after local variables");
                }
                
                // Generate ALGO (stop before 'return')
                generateFuncAlgoBlock();
                
                // Handle return statement
                if (match(TokenType.SEMICOLON)) {
                    // consume semicolon before return
                }
                expect(TokenType.RETURN, "Expected 'return' in function");
                String returnValue = parseAtom();
                emit("RETURN " + returnValue);
                
                expect(TokenType.RBRACE, "Expected '}' to end function body");
            } else {
                advance();
            }
        }
        
        expect(TokenType.RBRACE, "Expected '}' to end func block");
    }
    
    /**
     * Generate ALGO block for functions - stops when it encounters 'return' keyword.
     * This is different from regular ALGO blocks because in functions,
     * the 'return' statement is not part of ALGO.
     */
    private void generateFuncAlgoBlock() {
        // Read instructions until we hit 'return' or '}'
        while (!isAtEnd() && !check(TokenType.RBRACE) && !check(TokenType.RETURN)) {
            // Also stop if we see a semicolon followed by return
            if (check(TokenType.SEMICOLON) && lookAhead(TokenType.RETURN)) {
                break;
            }
            generateInstr();
            if (check(TokenType.SEMICOLON) && !lookAhead(TokenType.RETURN)) {
                advance();
            }
        }
    }

    /**
     * Parse parameter list: ( MAXTHREE )
     * PARAM ::= MAXTHREE
     * MAXTHREE ::= ε | VAR | VAR VAR | VAR VAR VAR
     */
    private List<String> parseParamList() {
        expect(TokenType.LPAREN, "Expected '(' to start parameter list");
        List<String> params = new ArrayList<>();
        
        // Up to 3 parameters
        while (!check(TokenType.RPAREN) && params.size() < 3) {
            if (check(TokenType.IDENTIFIER)) {
                params.add(advance().getLexeme());
            } else {
                break;
            }
        }
        
        expect(TokenType.RPAREN, "Expected ')' to end parameter list");
        return params;
    }

    /**
     * Skip variable declarations (used for local variables)
     */
    private void skipVariables() {
        // VARIABLES ::= ε | VAR VARIABLES
        while (!isAtEnd() && check(TokenType.IDENTIFIER) && !check(TokenType.RBRACE)) {
            advance(); // skip variable name
        }
    }

    /**
     * Parse a single ATOM (variable or number)
     */
    private String parseAtom() {
        if (check(TokenType.IDENTIFIER)) {
            return advance().getLexeme();
        }
        if (check(TokenType.NUMBER)) {
            return advance().getLexeme();
        }
        throw error("Expected ATOM (variable or number) at " + where());
    }

    /**
     * Join parameters for display purposes
     */
    private String joinParams(List<String> params) {
        if (params.isEmpty()) return "";
        StringJoiner j = new StringJoiner(", ");
        params.forEach(j::add);
        return " (" + j.toString() + ")";
    }

    private void generateMain() {
        expect(TokenType.LBRACE, "Expected '{' to start main block");
        // optional: var { ... }
        if (check(TokenType.VAR)) {
            advance(); // 'var'
            skipBraceBlock(); // ignore declarations
        }
        // Generate ALGO until '}' of main
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            generateInstr();
            if (check(TokenType.SEMICOLON)) advance();
        }
        expect(TokenType.RBRACE, "Expected '}' to end main block");
    }

    private void generateInstr() {
        // halt
        if (match(TokenType.HALT)) {
            emit("STOP");
            return;
        }

        // print OUTPUT
        if (match(TokenType.PRINT)) {
            String code = parseOutput();
            emit("PRINT " + code);
            return;
        }

        // branch: if TERM { ALGO } [ else { ALGO } ]
        if (match(TokenType.IF)) {
            expect(TokenType.LPAREN, "Expected '(' after if");
            Expr cond = parseTermExpr();
            expect(TokenType.RPAREN, "Expected ')' after if condition");

            String thenLabel = labels.newLabel("T");
            String exitLabel = labels.newLabel("E");

            expect(TokenType.LBRACE, "Expected '{' to start then-block");
            List<String> thenCode = withBuffer(() -> generateAlgoBlock());
            expect(TokenType.RBRACE, "Expected '}' to end then-block");

            boolean hasElse = false;
            List<String> elseCode = new ArrayList<>();
            if (match(TokenType.ELSE)) {
                hasElse = true;
                expect(TokenType.LBRACE, "Expected '{' to start else-block");
                elseCode = withBuffer(() -> generateAlgoBlock());
                expect(TokenType.RBRACE, "Expected '}' to end else-block");
            }

            // Per rules: translate if (not TERM) by swapping then/else
            if (cond.isNot) {
                cond = cond.left; // unwrap not
                if (hasElse) {
                    List<String> tmp = thenCode;
                    thenCode = elseCode;
                    elseCode = tmp;
                } else {
                    // No else: treat as else-only by moving then-code to else branch
                    hasElse = true;
                    elseCode = thenCode;
                    thenCode = new ArrayList<>();
                }
            }

            // Normal case
            if (hasElse) {
                emitIf(cond, thenLabel);
                emitList(elseCode);
                emit("GOTO " + exitLabel);
                emit("REM " + thenLabel);
                emitList(thenCode);
                emit("REM " + exitLabel);
            } else {
                emitIf(cond, thenLabel);
                emit("GOTO " + exitLabel);
                emit("REM " + thenLabel);
                emitList(thenCode);
                emit("REM " + exitLabel);
            }
            return;
        }

        // loops
        // while TERM { ALGO }
        if (match(TokenType.WHILE)) {
            expect(TokenType.LPAREN, "Expected '(' after while");
            Expr cond = parseTermExpr();
            expect(TokenType.RPAREN, "Expected ')' after while condition");
            String start = labels.newLabel("W");
            String body = labels.newLabel("WB");
            String exit = labels.newLabel("WE");
            expect(TokenType.LBRACE, "Expected '{' to start while-body");
            List<String> bodyCode = withBuffer(() -> generateAlgoBlock());
            expect(TokenType.RBRACE, "Expected '}' to end while-body");

            emit("REM " + start);
            emitIf(cond, body);
            emit("GOTO " + exit);
            emit("REM " + body);
            emitList(bodyCode);
            emit("GOTO " + start);
            emit("REM " + exit);
            return;
        }

        // do { ALGO } until TERM
        if (match(TokenType.DO)) {
            expect(TokenType.LBRACE, "Expected '{' after do");
            String bodyLabel = labels.newLabel("D");
            String exit = labels.newLabel("DE");
            List<String> bodyCode = withBuffer(() -> generateAlgoBlock());
            expect(TokenType.RBRACE, "Expected '}' to end do-body");
            expect(TokenType.UNTIL, "Expected 'until' after do-body");
            expect(TokenType.LPAREN, "Expected '(' after until");
            Expr cond = parseTermExpr();
            expect(TokenType.RPAREN, "Expected ')' after until condition");

            emit("REM " + bodyLabel);
            emitList(bodyCode);
            emitIf(cond, exit);
            emit("GOTO " + bodyLabel);
            emit("REM " + exit);
            return;
        }

        // Procedure call without return: NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookAhead(TokenType.LPAREN)) {
            String name = advance().getLexeme();
            List<String> args = parseInputList();
            emit("CALL " + name + joinArgs(args));
            return;
        }

        // Assignment: VAR = TERM  or VAR = NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookAhead(TokenType.ASSIGN)) {
            String lhs = advance().getLexeme();
            expect(TokenType.ASSIGN, "Expected '=' in assignment");
            // Function call with return? name(args)
            if (check(TokenType.IDENTIFIER) && lookAhead(TokenType.LPAREN)) {
                String fname = advance().getLexeme();
                List<String> args = parseInputList();
                emit("LET " + lhs + " = CALL " + fname + joinArgs(args));
                return;
            }
            Expr rhs = parseTermExpr();
            emit("LET " + lhs + " = " + exprToString(rhs));
            return;
        }

        throw error("Unrecognized instruction at " + where());
    }

    private void generateAlgoBlock() {
        // Read instructions until '}' (caller consumes the '}')
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            generateInstr();
            if (check(TokenType.SEMICOLON)) advance();
        }
    }

    // ---------- OUTPUT / TERM parsing ----------

    private String parseOutput() {
        // OUTPUT := string | ATOM
        if (check(TokenType.STRING)) {
            String s = advance().getLexeme();
            // Ensure output is quoted
            if (!s.startsWith("\"")) s = "\"" + s;
            if (!s.endsWith("\"")) s = s + "\"";
            return s;
        }
        // ATOM: var or number
        if (check(TokenType.IDENTIFIER)) {
            return advance().getLexeme();
        }
        if (check(TokenType.NUMBER)) {
            return advance().getLexeme();
        }
        throw error("Invalid OUTPUT at " + where());
    }

    // --- Expression mini-AST ---
    private static class Expr {
        String op;  // null => atom
        Expr left, right;
        String atom;
        boolean isNot = false;

        Expr(String atom) {
            this.atom = atom;
        }

        Expr(String op, Expr left, Expr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    private Expr parseTermExpr() {
        // TERM := ATOM | ( UNOP TERM ) | ( TERM BINOP TERM )
        if (check(TokenType.LPAREN)) {
            advance(); // (
            
            // Check for unary operators
            if (check(TokenType.NEG)) {
                advance(); // consume 'neg'
                Expr t = parseTermExpr();
                expect(TokenType.RPAREN, "Expected ')' after unary TERM");
                return new Expr("neg", t, null);
            }
            if (check(TokenType.NOT)) {
                advance(); // consume 'not'
                Expr t = parseTermExpr();
                expect(TokenType.RPAREN, "Expected ')' after unary TERM");
                Expr result = new Expr("not", t, null);
                result.isNot = true;
                return result;
            }
            
            // Otherwise assume binary
            Expr left = parseTermExpr();
            if (isBinOpPeek()) {
                String op = readBinOp();
                Expr right = parseTermExpr();
                expect(TokenType.RPAREN, "Expected ')' after binary TERM");
                return new Expr(op, left, right);
            }
            // Actually was a group: ( TERM )
            expect(TokenType.RPAREN, "Expected ')' to close group");
            return left;
        }
        
        if (check(TokenType.NUMBER)) {
            return new Expr(advance().getLexeme());
        }
        
        if (check(TokenType.IDENTIFIER)) {
            if (lookAhead(TokenType.LPAREN)) {
                String name = advance().getLexeme();
                List<String> args = parseInputList();
                String call = "CALL " + name + joinArgs(args);
                return new Expr(call);
            }
            return new Expr(advance().getLexeme());
        }
        
        throw error("Invalid TERM at " + where());
    }

    // Check if current token is a binary operator
    private boolean isBinOpPeek() {
        return check(TokenType.PLUS) || check(TokenType.MINUS) || 
               check(TokenType.MULT) || check(TokenType.DIV) ||
               check(TokenType.EQ) || check(TokenType.GT) || 
               check(TokenType.OR) || check(TokenType.AND);
    }

    // Consume BINOP and return normalized string
    private String readBinOp() {
        if (match(TokenType.PLUS)) return "plus";
        if (match(TokenType.MINUS)) return "minus";
        if (match(TokenType.MULT)) return "mult";
        if (match(TokenType.DIV)) return "div";
        if (match(TokenType.EQ)) return "eq";
        if (match(TokenType.GT)) return ">";
        if (match(TokenType.OR)) return "or";
        if (match(TokenType.AND)) return "and";
        throw error("Expected binary operator at " + where());
    }

    private void emitIf(Expr cond, String trueLabel) {
        // Lower boolean or/and by cascading IFs
        if ("or".equals(cond.op)) {
            // IF left THEN trueLabel
            emitIf(cond.left, trueLabel);
            // IF right THEN trueLabel
            emitIf(cond.right, trueLabel);
            return;
        }
        if ("and".equals(cond.op)) {
            // For AND: need an intermediate label
            String mid = labels.newLabel("C");
            emitIf(cond.left, mid);
            emit("REM " + mid);
            emitIf(cond.right, trueLabel);
            return;
        }
        // Base cases: eq, >, arithmetic comparisons, atom, call
        String condStr = exprToString(cond);
        emit("IF " + condStr + " THEN " + trueLabel);
    }

    private String exprToString(Expr e) {
        if (e == null) return "";
        if (e.op == null) {
            return e.atom;
        }
        if ("neg".equals(e.op)) {
            return "-" + exprToString(e.left);
        }
        if ("not".equals(e.op)) {
            return "NOT " + exprToString(e.left);
        }
        String op;
        switch (e.op) {
            case "plus": op = "+"; break;
            case "minus": op = "-"; break;
            case "mult": op = "*"; break;
            case "div": op = "/"; break;
            case "eq": op = "="; break;
            case ">": op = ">"; break;
            case "or": op = "OR"; break;
            case "and": op = "AND"; break;
            default: op = e.op; break;
        }
        return "(" + exprToString(e.left) + " " + op + " " + exprToString(e.right) + ")";
    }

    // ---------- helpers ----------

    private List<String> withBuffer(Runnable r) {
        int startLen = out.length();
        r.run();
        String block = out.substring(startLen);
        out.setLength(startLen);
        List<String> lines = new ArrayList<>();
        for (String line : block.split("\\R")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) lines.add(trimmed);
        }
        return lines;
    }

    private void emit(String line) {
        out.append(line).append(System.lineSeparator());
    }

    private void emitList(List<String> lines) {
        for (String l : lines) emit(l);
    }

    private List<String> parseInputList() {
        expect(TokenType.LPAREN, "Expected '(' to start argument list");
        List<String> args = new ArrayList<>();
        // Up to three params by grammar
        while (!check(TokenType.RPAREN)) {
            if (check(TokenType.IDENTIFIER)) {
                args.add(advance().getLexeme());
            } else if (check(TokenType.NUMBER)) {
                args.add(advance().getLexeme());
            } else {
                break;
            }
            if (check(TokenType.RPAREN)) break;
        }
        expect(TokenType.RPAREN, "Expected ')' to end argument list");
        return args;
    }

    private String joinArgs(List<String> args) {
        if (args.isEmpty()) return "";
        StringJoiner j = new StringJoiner(" ");
        args.forEach(j::add);
        return " " + j.toString();
    }

    private void skipBraceBlock() {
        expect(TokenType.LBRACE, "Expected '{'");
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (match(TokenType.LBRACE)) depth++;
            else if (match(TokenType.RBRACE)) depth--;
            else advance();
        }
        if (depth != 0) throw error("Unclosed block");
    }

    // Look ahead to next token type
    private boolean lookAhead(TokenType type) {
        int idx = current + 1;
        if (idx >= tokens.size()) return false;
        return tokens.get(idx).getType() == type;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) { 
            advance(); 
            return true; 
        }
        return false;
    }

    private void expect(TokenType type, String message) {
        if (!match(type)) {
            throw error(message + " at " + where() + ", found " + peek().getType());
        }
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private CodeGenerationException error(String msg) {
        return new CodeGenerationException(msg);
    }

    private String where() {
        Token t = peek();
        return t.getLine() + ":" + t.getColumn();
    }
}