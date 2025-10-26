package codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import lexer.Token;
import lexer.TokenType;

/**
 * SPL -> Target code generator that works directly with Token types.
 * Maps SPL variable names to valid BASIC variable names (single letters).
 */
public class CodeGenerator {

    private final List<Token> tokens;
    private int current = 0;
    private final StringBuilder out = new StringBuilder();
    private final LabelGenerator labels = new LabelGenerator();
    private final VariableMapper varMapper = new VariableMapper();

    public CodeGenerator(List<Token> tokens) {
        this.tokens = tokens;
    }

    public String generate() {
        // program ::= [glob block] [proc block] [func block] main block
        while (!isAtEnd()) {
            if (match(TokenType.GLOB)) {
                skipBraceBlock(); // ignore globals but register variable names
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
            advance();
        }
        return out.toString();
    }

    /**
     * Generate code for procedure definitions.
     */
    private void generateProcDefs() {
        expect(TokenType.LBRACE, "Expected '{' to start proc block");
        
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            if (check(TokenType.IDENTIFIER)) {
                String procName = advance().getLexeme();
                
                // Parse parameters and map them
                List<String> params = parseParamList();
                List<String> mappedParams = new ArrayList<>();
                for (String param : params) {
                    mappedParams.add(varMapper.get(param));
                }
                
                // Generate procedure header
                emit("REM PROC " + procName + joinParams(mappedParams));
                
                expect(TokenType.LBRACE, "Expected '{' to start procedure body");
                
                // Optional local variables
                if (match(TokenType.LOCAL)) {
                    expect(TokenType.LBRACE, "Expected '{' after local");
                    registerLocalVariables();
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
     */
    private void generateFuncDefs() {
        expect(TokenType.LBRACE, "Expected '{' to start func block");
        
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            if (check(TokenType.IDENTIFIER)) {
                String funcName = advance().getLexeme();
                
                // Parse parameters and map them
                List<String> params = parseParamList();
                List<String> mappedParams = new ArrayList<>();
                for (String param : params) {
                    mappedParams.add(varMapper.get(param));
                }
                
                // Generate function header
                emit("REM FUNC " + funcName + joinParams(mappedParams));
                
                expect(TokenType.LBRACE, "Expected '{' to start function body");
                
                // Optional local variables
                if (match(TokenType.LOCAL)) {
                    expect(TokenType.LBRACE, "Expected '{' after local");
                    registerLocalVariables();
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
     */
    private void generateFuncAlgoBlock() {
        while (!isAtEnd() && !check(TokenType.RBRACE) && !check(TokenType.RETURN)) {
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
     * Parse parameter list and return SPL names (will be mapped later).
     */
    private List<String> parseParamList() {
        expect(TokenType.LPAREN, "Expected '(' to start parameter list");
        List<String> params = new ArrayList<>();
        
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
     * Register local variables with the mapper (just consume and register them).
     */
    private void registerLocalVariables() {
        while (!isAtEnd() && check(TokenType.IDENTIFIER) && !check(TokenType.RBRACE)) {
            String varName = advance().getLexeme();
            // Register the variable (assigns a BASIC name)
            varMapper.get(varName);
        }
    }

    /**
     * Parse a single ATOM (variable or number) and map if it's a variable.
     */
    private String parseAtom() {
        if (check(TokenType.IDENTIFIER)) {
            String varName = advance().getLexeme();
            return varMapper.get(varName);
        }
        if (check(TokenType.NUMBER)) {
            return advance().getLexeme();
        }
        throw error("Expected ATOM (variable or number) at " + where());
    }

    /**
     * Join parameters for display purposes.
     */
    private String joinParams(List<String> params) {
        if (params.isEmpty()) return "";
        StringJoiner j = new StringJoiner(", ");
        params.forEach(j::add);
        return " (" + j.toString() + ")";
    }

    private void generateMain() {
        expect(TokenType.LBRACE, "Expected '{' to start main block");
        if (check(TokenType.VAR)) {
            advance(); // 'var'
            skipBraceBlock(); // ignore declarations
        }
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
            Expr cond = parseTermExpr();

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

            // Handle 'not' by swapping branches
            if (cond.isNot) {
                cond = cond.left;
                if (hasElse) {
                    List<String> tmp = thenCode;
                    thenCode = elseCode;
                    elseCode = tmp;
                } else {
                    hasElse = true;
                    elseCode = thenCode;
                    thenCode = new ArrayList<>();
                }
            }

            // Emit IF structure
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

        // while TERM { ALGO }
        if (match(TokenType.WHILE)) {
            Expr cond = parseTermExpr();
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
            Expr cond = parseTermExpr();

            emit("REM " + bodyLabel);
            emitList(bodyCode);
            emitIf(cond, exit);
            emit("GOTO " + bodyLabel);
            emit("REM " + exit);
            return;
        }

        // Procedure call: NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookAhead(TokenType.LPAREN)) {
            String name = advance().getLexeme();
            List<String> args = parseInputList();
            emit("CALL " + name + joinArgs(args));
            return;
        }

        // Assignment: VAR = TERM or VAR = NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookAhead(TokenType.ASSIGN)) {
            String lhs = advance().getLexeme();
            String mappedLhs = varMapper.get(lhs);
            expect(TokenType.ASSIGN, "Expected '=' in assignment");
            
            // Function call with return?
            if (check(TokenType.IDENTIFIER) && lookAhead(TokenType.LPAREN)) {
                String fname = advance().getLexeme();
                List<String> args = parseInputList();
                emit("LET " + mappedLhs + " = CALL " + fname + joinArgs(args));
                return;
            }
            Expr rhs = parseTermExpr();
            emit("LET " + mappedLhs + " = " + exprToString(rhs));
            return;
        }

        throw error("Unrecognized instruction at " + where());
    }

    private void generateAlgoBlock() {
        while (!isAtEnd() && !check(TokenType.RBRACE)) {
            generateInstr();
            if (check(TokenType.SEMICOLON)) advance();
        }
    }

    /**
     * Parse OUTPUT and map variables.
     */
    private String parseOutput() {
        if (check(TokenType.STRING)) {
            String s = advance().getLexeme();
            if (!s.startsWith("\"")) s = "\"" + s;
            if (!s.endsWith("\"")) s = s + "\"";
            return s;
        }
        if (check(TokenType.IDENTIFIER)) {
            String varName = advance().getLexeme();
            return varMapper.get(varName);
        }
        if (check(TokenType.NUMBER)) {
            return advance().getLexeme();
        }
        throw error("Invalid OUTPUT at " + where());
    }

    // --- Expression mini-AST ---
    private static class Expr {
        String op;
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
        if (check(TokenType.LPAREN)) {
            advance(); // (
            
            // Check for unary operators
            if (check(TokenType.NEG)) {
                advance();
                Expr t = parseTermExpr();
                expect(TokenType.RPAREN, "Expected ')' after unary TERM");
                return new Expr("neg", t, null);
            }
            if (check(TokenType.NOT)) {
                advance();
                Expr t = parseTermExpr();
                expect(TokenType.RPAREN, "Expected ')' after unary TERM");
                Expr result = new Expr("not", t, null);
                result.isNot = true;
                return result;
            }
            
            // Binary expression
            Expr left = parseTermExpr();
            if (isBinOpPeek()) {
                String op = readBinOp();
                Expr right = parseTermExpr();
                expect(TokenType.RPAREN, "Expected ')' after binary TERM");
                return new Expr(op, left, right);
            }
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
            // Map variable name
            String varName = advance().getLexeme();
            return new Expr(varMapper.get(varName));
        }
        
        throw error("Invalid TERM at " + where());
    }

    private boolean isBinOpPeek() {
        return check(TokenType.PLUS) || check(TokenType.MINUS) || 
               check(TokenType.MULT) || check(TokenType.DIV) ||
               check(TokenType.EQ) || check(TokenType.GT) || 
               check(TokenType.OR) || check(TokenType.AND);
    }

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
        if ("or".equals(cond.op)) {
            emitIf(cond.left, trueLabel);
            emitIf(cond.right, trueLabel);
            return;
        }
        if ("and".equals(cond.op)) {
            String mid = labels.newLabel("C");
            emitIf(cond.left, mid);
            emit("REM " + mid);
            emitIf(cond.right, trueLabel);
            return;
        }
        String condStr = exprToString(cond);
        emit("IF " + condStr + " THEN " + trueLabel);
    }

    /**
     * Convert expression to string. Variables are already mapped in the Expr.
     */
    private String exprToString(Expr e) {
        if (e == null) return "";
        if (e.op == null) {
            return e.atom;  // Already mapped
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

    /**
     * Parse input list and map variable arguments.
     */
    private List<String> parseInputList() {
        expect(TokenType.LPAREN, "Expected '(' to start argument list");
        List<String> args = new ArrayList<>();
        
        while (!check(TokenType.RPAREN)) {
            if (check(TokenType.IDENTIFIER)) {
                String varName = advance().getLexeme();
                args.add(varMapper.get(varName));
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