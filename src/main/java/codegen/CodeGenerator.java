package codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import lexer.Token;
import lexer.TokenType;

/**
 * Minimal SPL -> Target code generator based on the provided translation rules.
 * Walks the token list directly (post-parse) and emits target code lines.
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
            if (matchLex("glob")) {
                skipBraceBlock(); // ignore globals
                continue;
            }
            if (matchLex("proc")) {
                skipBraceBlock(); // definitions reserved for inlining
                continue;
            }
            if (matchLex("func")) {
                skipBraceBlock(); // definitions reserved for inlining
                continue;
            }
            if (matchLex("main")) {
                generateMain();
                break;
            }
            // Anything else: advance to avoid infinite loop
            advance();
        }
        return out.toString();
    }

    private void generateMain() {
        expectLex("{", "Expected '{' to start main block");
        // optional: var { ... }
        if (checkLex("var")) {
            advance(); // 'var'
            skipBraceBlock(); // ignore declarations
        }
        // Generate ALGO until '}' of main
        while (!isAtEnd() && !checkLex("}")) {
            // Some inputs end without ';' on last INSTR; accept optional semicolons
            generateInstr();
            if (checkLex(";")) advance();
        }
        expectLex("}", "Expected '}' to end main block");
    }

    private void generateInstr() {
        // halt
        if (matchLex("halt")) {
            emit("STOP");
            return;
        }

        // print OUTPUT
        if (matchLex("print")) {
            String code = parseOutput();
            emit("PRINT " + code);
            return;
        }

        // branch: if TERM { ALGO } [ else { ALGO } ]
        if (matchLex("if")) {
            expectLex("(", "Expected '(' after if");
            Expr cond = parseTermExpr();
            expectLex(")", "Expected ')' after if condition");

            String thenLabel = labels.newLabel("T");
            String exitLabel = labels.newLabel("E");

            expectLex("{", "Expected '{' to start then-block");
            List<String> thenCode = withBuffer(() -> generateAlgoBlock());
            expectLex("}", "Expected '}' to end then-block");

            boolean hasElse = false;
            List<String> elseCode = new ArrayList<>();
            if (matchLex("else")) {
                hasElse = true;
                expectLex("{", "Expected '{' to start else-block");
                elseCode = withBuffer(() -> generateAlgoBlock());
                expectLex("}", "Expected '}' to end else-block");
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
        if (matchLex("while")) {
            expectLex("(", "Expected '(' after while");
            Expr cond = parseTermExpr();
            expectLex(")", "Expected ')' after while condition");
            String start = labels.newLabel("W");
            String body = labels.newLabel("WB");
            String exit = labels.newLabel("WE");
            expectLex("{", "Expected '{' to start while-body");
            List<String> bodyCode = withBuffer(() -> generateAlgoBlock());
            expectLex("}", "Expected '}' to end while-body");

            emit("REM " + start);
            // For OR/AND we cascade using emitIf; if true -> body, else will fall-through to exit jump
            emitIf(cond, body);
            emit("GOTO " + exit);
            emit("REM " + body);
            emitList(bodyCode);
            emit("GOTO " + start);
            emit("REM " + exit);
            return;
        }

        // do { ALGO } until TERM
        if (matchLex("do")) {
            expectLex("{", "Expected '{' after do");
            String bodyLabel = labels.newLabel("D");
            String exit = labels.newLabel("DE");
            List<String> bodyCode = withBuffer(() -> generateAlgoBlock());
            expectLex("}", "Expected '}' to end do-body");
            expectLex("until", "Expected 'until' after do-body");
            expectLex("(", "Expected '(' after until");
            Expr cond = parseTermExpr();
            expectLex(")", "Expected ')' after until condition");

            emit("REM " + bodyLabel);
            emitList(bodyCode);
            // If condition true -> exit, else -> repeat
            // We cannot invert IF easily; emit: IF cond THEN exit; GOTO bodyLabel
            emitIf(cond, exit);
            emit("GOTO " + bodyLabel);
            emit("REM " + exit);
            return;
        }

        // Procedure call without return: NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookAheadLex("(")) {
            String name = advance().getLexeme();
            List<String> args = parseInputList();
            emit("CALL " + name + joinArgs(args));
            return;
        }

        // Assignment: VAR = TERM  or VAR = NAME ( INPUT )
        if (check(TokenType.IDENTIFIER) && lookAheadLex("=")) {
            String lhs = advance().getLexeme();
            expectLex("=", "Expected '=' in assignment");
            // Function call with return? name(args)
            if (check(TokenType.IDENTIFIER) && lookAheadLex("(")) {
                String fname = advance().getLexeme();
                List<String> args = parseInputList();
                emit(lhs + " = CALL " + fname + joinArgs(args));
                return;
            }
            Expr rhs = parseTermExpr();
            emit(lhs + " = " + exprToString(rhs));
            return;
        }

        throw error("Unrecognized instruction at " + where());
    }

    private void generateAlgoBlock() {
        // Read instructions until '}' (caller consumes the '}')
        while (!isAtEnd() && !checkLex("}")) {
            generateInstr();
            if (checkLex(";")) advance();
        }
    }

    // ---------- OUTPUT / TERM parsing ----------

    private String parseOutput() {
        // OUTPUT := string | ATOM
        if (check(TokenType.STRING)) {
            String s = advance().getLexeme();
            // Ensure output is quoted
            if (!(s.startsWith("\"") && s.endsWith("\""))) {
                s = "\"" + s + "\"";
            }
            return s;
        }
        // ATOM ::= number | VAR
        if (check(TokenType.NUMBER)) {
            return advance().getLexeme();
        }
        if (check(TokenType.IDENTIFIER)) {
            return advance().getLexeme(); // already renamed (per assumptions)
        }
        throw error("Invalid OUTPUT at " + where());
    }

    private static class Expr {
        // Simple expression tree for TERM
        final String op; // null for ATOM; "plus","minus","mult","div","eq",">","or","and","neg","not"
        final Expr left;
        final Expr right;
        final String atom; // number | identifier
        final boolean isNot;
        Expr(String atom) { this.atom = atom; this.op = null; this.left = null; this.right = null; this.isNot = false; }
        Expr(String op, Expr left, Expr right) {
            this.op = op; this.left = left; this.right = right; this.atom = null; this.isNot = "not".equals(op);
        }
    }

    private Expr parseTermExpr() {
        // TERM := primary (BINOP TERM)?
        Expr left = parsePrimary();
        if (isBinOpPeek()) {
            String op = readBinOp();
            Expr right = parseTermExpr(); // right-recursive; fine for our grammar
            return new Expr(op, left, right);
        }
        return left;
    }
 
    // primary := number | identifier | CALL | ( UNOP TERM ) | ( TERM )
    private Expr parsePrimary() {
        if (matchLex("(")) {
            if (checkLex("neg") || checkLex("not")) {
                String op = advance().getLexeme();
                Expr t = parseTermExpr();
                expectLex(")", "Expected ')' after unary TERM");
                return new Expr(op, t, null);
            }
            Expr e = parseTermExpr();
            expectLex(")", "Expected ')' to close group");
            return e;
        }
        if (check(TokenType.NUMBER)) {
            return new Expr(advance().getLexeme());
        }
        if (check(TokenType.IDENTIFIER)) {
            if (lookAheadLex("(")) {
                String name = advance().getLexeme();
                List<String> args = parseInputList();
                String call = "CALL " + name + joinArgs(args);
                return new Expr(call);
            }
            return new Expr(advance().getLexeme());
        }
        throw error("Invalid TERM at " + where());
    }

    // Prefer token-type checks to avoid lexeme edge cases
    private boolean isBinOpPeek() {
        return check(TokenType.PLUS) || check(TokenType.MINUS) || check(TokenType.MULT) || check(TokenType.DIV)
            || check(TokenType.EQ) || check(TokenType.GT) || check(TokenType.OR) || check(TokenType.AND)
            || checkLex("plus") || checkLex("minus") || checkLex("mult") || checkLex("div")
            || checkLex("eq") || checkLex(">") || checkLex("or") || checkLex("and");
    }

    // Consume BINOP and normalize to the string the emitter expects
    private String readBinOp() {
        if (match(TokenType.PLUS) || matchLex("plus")) return "plus";
        if (match(TokenType.MINUS) || matchLex("minus")) return "minus";
        if (match(TokenType.MULT) || matchLex("mult")) return "mult";
        if (match(TokenType.DIV) || matchLex("div")) return "div";
        if (match(TokenType.EQ) || matchLex("eq")) return "eq";
        if (match(TokenType.GT) || matchLex(">")) return ">";
        if (match(TokenType.OR) || matchLex("or")) return "or";
        if (match(TokenType.AND) || matchLex("and")) return "and";
        throw error("Expected binary operator at " + where());
    }

    private void emitIf(Expr cond, String trueLabel) {
        // Lower boolean or/and by cascading IFs per the guidelines.
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
            // IF left THEN mid ; else fall-through
            emitIf(cond.left, mid);
            // If left false, nothing else to do (falls through)
            // If left true, continue with right:
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
            // 'not' is handled in control structures; fall back to a pseudo form
            return "NOT " + exprToString(e.left);
        }
        String op;
        switch (e.op) {
            case "plus":
                op = "+";
                break;
            case "minus":
                op = "-";
                break;
            case "mult":
                op = "*";
                break;
            case "div":
                op = "/";
                break;
            case "eq":
                op = "=";
                break;
            case ">":
                op = ">";
                break;
            case "or":
                op = "OR";
                break;
            case "and":
                op = "AND";
                break;
            default:
                op = e.op;
                break;
        }
        return "(" + exprToString(e.left) + " " + op + " " + exprToString(e.right) + ")";
    }

    // ---------- helpers ----------

    private List<String> withBuffer(Runnable r) {
        int startLen = out.length();
        r.run();
        String block = out.substring(startLen);
        // reset builder to previous state
        out.setLength(startLen);
        // split into lines
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
        expectLex("(", "Expected '(' to start argument list");
        List<String> args = new ArrayList<>();
        // Up to three params by grammar, whitespace-separated or with optional separators
        while (!checkLex(")")) {
            if (check(TokenType.IDENTIFIER)) {
                args.add(advance().getLexeme());
            } else if (check(TokenType.NUMBER)) {
                args.add(advance().getLexeme());
            } else {
                // Skip unexpected separators
            }
            // In this SPL, parameters appear separated by spaces; no commas
            if (checkLex(")")) break;
        }
        expectLex(")", "Expected ')' to end argument list");
        return args;
    }

    private String joinArgs(List<String> args) {
        if (args.isEmpty()) return "";
        StringJoiner j = new StringJoiner(" ");
        args.forEach(j::add);
        return " " + j.toString();
    }

    private void skipBraceBlock() {
        expectLex("{", "Expected '{'");
        int depth = 1;
        while (!isAtEnd() && depth > 0) {
            if (matchLex("{")) depth++;
            else if (matchLex("}")) depth--;
            else advance();
        }
        if (depth != 0) throw error("Unclosed block");
    }

    private boolean matchLex(String lex) {
        if (checkLex(lex)) { advance(); return true; }
        return false;
    }

    private boolean lookAheadLex(String lex) {
        int idx = current + 1;
        if (idx >= tokens.size()) return false;
        Token t = tokens.get(idx);
        return lex.equals(t.getLexeme());
    }

    private boolean checkLex(String lex) {
        if (isAtEnd()) return false;
        return lex.equals(peek().getLexeme());
    }

    private void expectLex(String lex, String message) {
        if (!matchLex(lex)) throw error(message + " at " + where());
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }

    private boolean match(TokenType type) {
        if (check(type)) { advance(); return true; }
        return false;
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