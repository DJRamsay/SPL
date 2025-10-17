package parser;

import java.util.List;

import lexer.Token;
import lexer.TokenType;

public class SyntaxTreeBuilder {
    private final List<Token> tokens;
    private int current = 0;

    public SyntaxTreeBuilder(List<Token> tokens) {
        this.tokens = tokens;
    }

    public SyntaxNode build() {
        SyntaxNode root = new SyntaxNode("Program");
        while (!isAtEnd()) {
            if (peek().getType() == TokenType.EOF) break;
            root.addChild(parseElement());
        }
        return root;
    }

    private SyntaxNode parseElement() {
        if (isAtEnd()) return new SyntaxNode("EOF");

        Token t = peek();

        // Block: { ... }
        if (t.getType() == TokenType.LBRACE) {
            advance(); // consume '{'
            SyntaxNode block = new SyntaxNode("{");
            while (!isAtEnd() && peek().getType() != TokenType.RBRACE) {
                block.addChild(parseElement());
            }
            if (!isAtEnd() && peek().getType() == TokenType.RBRACE) {
                // Add closing brace as a child node for visibility
                block.addChild(new SyntaxNode("}"));
                advance(); // consume '}'
            }
            return block;
        }

        // Parenthesis: ( ... )
        if (t.getType() == TokenType.LPAREN) {
            advance(); // consume '('
            SyntaxNode par = new SyntaxNode("(");
            while (!isAtEnd() && peek().getType() != TokenType.RPAREN) {
                par.addChild(parseElement());
            }
            if (!isAtEnd() && peek().getType() == TokenType.RPAREN) {
                par.addChild(new SyntaxNode(")"));
                advance(); // consume ')'
            }
            return par;
        }

        // Keywords and identifiers: make a labeled node, attach following block/paren if present
        if (isKeywordOrName(t.getType())) {
            Token head = advance();
            String lex = head.getLexeme();
            String label = (lex == null || lex.isEmpty()) ? head.getType().name() : lex + " (" + head.getType() + ")";
            SyntaxNode node = new SyntaxNode(label);
            if (!isAtEnd() && (peek().getType() == TokenType.LBRACE || peek().getType() == TokenType.LPAREN)) {
                node.addChild(parseElement());
            }
            return node;
        }

        // Fallback: leaf token
        Token leaf = advance();
        String leafLabel = (leaf.getLexeme() == null || leaf.getLexeme().isEmpty())
                ? leaf.getType().name()
                : leaf.getLexeme() + " (" + leaf.getType() + ")";
        return new SyntaxNode(leafLabel);
    }

    private boolean isKeywordOrName(TokenType t) {
        switch (t) {
            case GLOB:
            case PROC:
            case FUNC:
            case MAIN:
            case VAR:
            case LOCAL:
            case IF:
            case ELSE:
            case WHILE:
            case DO:
            case UNTIL:
            case PRINT:
            case HALT:
            case RETURN:
            case IDENTIFIER:
            case NUMBER:
            case STRING:
                return true;
            default:
                return false;
        }
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }
}
